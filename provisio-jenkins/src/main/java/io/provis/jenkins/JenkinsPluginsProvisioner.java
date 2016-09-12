package io.provis.jenkins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionConstraint;
import org.eclipse.aether.version.VersionScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import io.provis.MavenProvisioner;
import io.provis.ProvisioningException;
import io.provis.jenkins.aether.ResolutionSystem;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningContext;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Runtime;

public class JenkinsPluginsProvisioner {

  private static final Logger log = LoggerFactory.getLogger(JenkinsPluginsProvisioner.class);

  private static final String GROUP_PREFIX = "org.jenkins-ci.";
  private static final String OLD_GROUP_PREFIX = "org.jvnet.hudson.";
  public static final String DEFAULT_GROUP_ID = GROUP_PREFIX + "plugins";

  private ResolutionSystem resolution;
  private RepositorySystemSession session;
  private MavenProvisioner provisioner;
  private VersionScheme versions;

  public JenkinsPluginsProvisioner(ResolutionSystem resolution, RepositorySystemSession session) {
    this.resolution = resolution;
    this.session = session;
    this.provisioner = new MavenProvisioner(resolution.repositorySystem(), session, resolution.remoteRepositories());;
    this.versions = new GenericVersionScheme();
  }

  public void provision(JenkinsPluginsRequest req) throws Exception {
    PluginContext ctx = new PluginContext(req.getJenkinsVersion(), req.getBundledPlugins());
    for (JenkinsPlugin p : req.getPlugins()) {
      PluginDesc desc = ctx.describe(new DefaultArtifact(p.getGroupId(), p.getArtifactId(), null, p.getVersion()));
      if (desc == null) {
        throw new ProvisioningException("Cannot find plugin " + p.getGroupId() + ":" + p.getArtifactId() + ":" + p.getVersion());
      }

      ctx.setPlugin(desc.key, new PluginHolder(desc, p));
    }

    List<PluginHolder> topLevel = new ArrayList<>(ctx.plugins.values());

    // first, build a tree of all dependencies using a 'highest version' strategy
    for (PluginHolder h : topLevel) {
      processPluginDependencies(ctx, h);
    }

    // second, decide which to include based on optionality and also check for redundant dependencies
    List<PluginHolder> included = new ArrayList<>();
    Set<String> mem = new HashSet<>();
    for (PluginHolder h : topLevel) {
      log.info(" * {}:{}", h.plugin.key, h.plugin.art.getVersion());
      collect(ctx, h, h.pinned.isIncludeOptional(), included, mem, "   ->");
    }

    // collect included plugins
    req.getTargetDir().mkdirs();
    ArtifactSet arts = new ArtifactSet();
    for (PluginHolder h : included) {
      PluginDesc p = h.plugin;

      String bundled = ctx.getBundledVersion(p.key);
      if (bundled != null && compareVersions(p.art.getVersion(), bundled) <= 0) { // bundled version is same or higher than required
        log.info("Skipping {}:{} since version {} is bundled with jenkins", p.key, p.art.getVersion(), bundled);
        continue;
      }

      if (h.redundant) {
        log.info("Possibly redundant pinned plugin {}:{}", h.getArtifactId(), h.getVersion());
      }

      // error out if some plugin requires version of jenkins higher than provided
      if (compareVersions(p.jenkinsVersion, req.getJenkinsVersion()) > 0) {
        ctx.error("Plugin %s:%s requires jenkins version %s, which is less than the provisioned %s",
          p.art.getArtifactId(), p.art.getVersion(), p.jenkinsVersion, req.getJenkinsVersion());
      }

      ProvisioArtifact pa = new ProvisioArtifact(new DefaultArtifact(p.art.getGroupId(), p.art.getArtifactId(), "hpi", p.art.getVersion()));
      pa.setName(p.key + ".jpi");
      arts.addArtifact(pa);
    }

    if (!ctx.errors.isEmpty()) {
      StringBuilder sb = new StringBuilder("Errors encountered during plugins provisioning:");
      for (String error : ctx.errors) {
        sb.append("\n * ").append(error);
      }
      throw new ProvisioningException(sb.toString());
    }

    // perform provisioning
    Runtime runtime = new Runtime();
    arts.setDirectory("/");
    runtime.addArtifactSet(arts);

    ProvisioningRequest preq = new ProvisioningRequest();
    preq.setRuntimeDescriptor(runtime);
    preq.setOutputDirectory(req.getTargetDir());
    provisioner.provision(preq);

    // pin top-level plugins
    for (PluginHolder h : included) {
      if (h.pinned != null) {
        new File(req.getTargetDir(), h.plugin.key + ".jpi.pinned").createNewFile();
      }
    }

  }

  private void collect(PluginContext ctx, PluginHolder h, boolean optional, List<PluginHolder> result, Set<String> mem, String indent) {
    if (!mem.add(h.plugin.key)) {
      return;
    }
    result.add(h);
    if (h.dependencies != null) {
      for (DepHolder dep : h.dependencies) {
        if (!dep.optional || optional) {
          PluginHolder deph = ctx.getPlugin(dep.key);
          String depVersion = dep.version;
          String actualVersion = deph.plugin.art.getVersion();

          if (depVersion.equals(actualVersion)) {
            log.info("{} {}:{}", indent, dep.key, depVersion);
          } else {
            log.info("{} {}:{} ({})", indent, dep.key, depVersion, actualVersion);
          }
          collect(ctx, deph, optional, result, mem, "   " + indent);
        }
      }
    }
  }

  private void processPlugin(PluginContext ctx, PluginHolder parent, PluginDesc desc) throws RepositoryException, IOException {
    boolean updateDeps = false;

    PluginHolder holder = ctx.getPlugin(desc.key);
    if (holder == null) {
      holder = new PluginHolder(desc, null);
      ctx.setPlugin(desc.key, holder);
      updateDeps = true;
    } else if (mergeVersion(ctx, parent, holder, desc) || holder.dependencies == null) {
      updateDeps = true;
    }

    if (updateDeps) {
      processPluginDependencies(ctx, holder);
    }
  }

  private void processPluginDependencies(PluginContext ctx, PluginHolder holder) throws RepositoryException, IOException {
    if (holder.dependencies != null) {
      holder.dependencies.clear();
    } else {
      holder.dependencies = new HashSet<>();
    }

    for (PluginDepDesc dep : getDependencies(ctx, holder.plugin)) {
      PluginDesc depPlugin = dep.plugin;

      holder.dependencies.add(new DepHolder(depPlugin.key, depPlugin.art.getVersion(), dep.optional));
      processPlugin(ctx, holder, depPlugin);
    }
  }

  private boolean mergeVersion(PluginContext ctx, PluginHolder parent, PluginHolder holder, PluginDesc plugin) throws RepositoryException {
    String version = holder.getVersion();
    if (version == null) {
      holder.plugin = plugin;
      return true;
    }
    String newVersion = plugin.art.getVersion();
    int c = compareVersions(newVersion, version);

    if (c >= 0 && holder.pinned != null) {
      holder.redundant = true;
    }

    if (c > 0) {
      if (holder.pinned != null) {
        // error out if a transitive dependency plugin is of higher version than the one pinned
        if (parent != null) {
          ctx.error("Plugin %s:%s depends on %s:%s, but the pinned version is %s",
            parent.getArtifactId(), parent.getVersion(), holder.getArtifactId(), newVersion, version);
        }
      }

      holder.plugin = plugin;
      return true;
    }
    return false;
  }

  private PluginDesc doDescribe(PluginContext ctx, Artifact art) throws RepositoryException, IOException {

    // convert version range into fixed version
    VersionConstraint vc = versions.parseVersionConstraint(art.getVersion());
    if (vc.getRange() != null) {
      String newVersion = vc.getRange().getLowerBound().getVersion().toString();
      art = art.setVersion(newVersion);
    }

    boolean defaultGroup = art.getGroupId().startsWith(GROUP_PREFIX);

    ArtifactDescriptorRequest req = new ArtifactDescriptorRequest(art, resolution.remoteRepositories(), "provision");
    ArtifactDescriptorException origEx = null;
    ArtifactDescriptorResult res = null;
    try {
      res = resolution.getDescriptorReader().readArtifactDescriptor(session, req);
    } catch (ArtifactDescriptorException e) {
      origEx = e;
    }

    // retry org.jenkins-ci.* artifacts as org.jvnet.hudson.*
    if ((res == null || res.getProperties().isEmpty()) && defaultGroup) {
      String oldGroupId = OLD_GROUP_PREFIX + art.getGroupId().substring(GROUP_PREFIX.length());
      Artifact oldArt = new DefaultArtifact(oldGroupId, art.getArtifactId(), art.getExtension(), art.getVersion());
      req.setArtifact(oldArt);
      try {
        res = resolution.getDescriptorReader().readArtifactDescriptor(session, req);
      } catch (ArtifactDescriptorException e2) {
        throw origEx;
      }
    }
    if ((res == null || res.getProperties().isEmpty())) {
      if (origEx != null) {
        throw origEx;
      }
      return null;
    }

    String packaging = (String) res.getProperties().get("packaging");
    if (packaging == null || !TYPES.contains(packaging)) {
      return null;
    }

    art = res.getArtifact();

    File file = resolve(ctx, art.getGroupId(), art.getArtifactId(), art.getVersion(), "jar").getFile();
    Manifest m;
    try (JarFile jar = new JarFile(file)) {
      m = jar.getManifest();
    }
    Attributes attrs = m.getMainAttributes();
    String key = attrs.getValue("Short-Name");

    String jenkinsVersion = attrs.getValue("Jenkins-Version");
    if (jenkinsVersion == null) {
      jenkinsVersion = attrs.getValue("Hudson-Version");
    }
    if (jenkinsVersion.equals("null")) {
      jenkinsVersion = null;
    }

    return new PluginDesc(key, art, res.getDependencies(), jenkinsVersion);
  }

  private List<PluginDepDesc> getDependencies(PluginContext ctx, PluginDesc plugin) throws RepositoryException, IOException {

    List<PluginDepDesc> deps = new ArrayList<>();
    Set<String> added = new HashSet<>();

    // collect plugin depdencies from pom
    for (Dependency d : plugin.deps) {
      String scope = d.getScope();
      if (scope == null || SCOPES.contains(scope)) {
        PluginDesc depPlugin = ctx.describe(d.getArtifact());
        if (depPlugin != null && !breakCycle(plugin, depPlugin)) {
          added.add(depPlugin.key);
          deps.add(new PluginDepDesc(depPlugin, d.isOptional()));
        }
      }
    }

    // add detached plugins
    for (Map.Entry<String, DetachedPlugin> e : DETACHED.entrySet()) {
      String key = e.getKey(); // key == artifactId == shortName

      DetachedPlugin det = e.getValue();
      if (!key.equals(plugin.key) && !added.contains(key) && needsDetachedPlugin(ctx, plugin, det.splitWhen)) {
        added.add(key);
        PluginDesc depPlugin = ctx.describe(new DefaultArtifact(det.groupId, key, null, det.requireVersion));
        if (depPlugin == null) {
          throw new ProvisioningException("Cannot fetch detached plugin " + key + ":" + det.requireVersion);
        }
        deps.add(new PluginDepDesc(depPlugin, false));
      }
    }

    return deps;
  }

  private boolean needsDetachedPlugin(PluginContext ctx, PluginDesc plugin, String splitWhen) throws RepositoryException {
    return compareVersions(ctx.getJenkinsVersion(), splitWhen) >= 0 // provisioned jenkins no longer contains the detached plugin
      && compareVersions(plugin.jenkinsVersion, splitWhen) < 0; // plugin requires pre-split version of jenkins
  }

  private boolean breakCycle(PluginDesc plugin, PluginDesc depPlugin) {
    Set<String> s = BREAK_CYCLES.get(plugin.key);
    return s != null && s.contains(depPlugin.key);
  }

  private int compareVersions(String ver1, String ver2) throws RepositoryException {
    if (ver1.equals(ver2)) {
      return 0;
    }
    return versions.parseVersion(ver1).compareTo(versions.parseVersion(ver2));
  }

  private Artifact resolve(PluginContext ctx, String groupId, String artifactId, String version, String type) throws RepositoryException {

    ProvisioArtifact art = new ProvisioArtifact(new DefaultArtifact(groupId, artifactId, type, version));
    art.addExclusion("*:*");

    ProvisioningRequest preq = new ProvisioningRequest();
    ProvisioningResult pres = new ProvisioningResult(preq);
    ProvisioningContext pctx = new ProvisioningContext(preq, pres);

    Set<ProvisioArtifact> results = provisioner.resolveArtifact(pctx, art);
    if (results.isEmpty()) {
      throw new ProvisioningException("Cannot resolve artifact " + groupId + ":" + artifactId + ":" + type + ":" + version);
    }

    return results.iterator().next();
  }

  private static final Set<String> SCOPES = ImmutableSet.of("", "compile", "runtime");
  private static final Set<String> TYPES = ImmutableSet.of("jpi", "hpi");

  private static final Map<String, Set<String>> BREAK_CYCLES = ImmutableMap.<String, Set<String>>builder()
    .put("script-security", ImmutableSet.of("matrix-auth", "windows-slaves", "antisamy-markup-formatter", "matrix-project"))
    .put("credentials", ImmutableSet.of("matrix-auth", "windows-slaves"))
    .build();

  private static final Map<String, DetachedPlugin> DETACHED = ImmutableMap.<String, DetachedPlugin>builder()
    .put("maven-plugin", new DetachedPlugin("1.297", "1.296", "org.jenkins-ci.main"))
    .put("subversion", new DetachedPlugin("1.311", "1.0"))
    .put("cvs", new DetachedPlugin("1.341", "0.1"))
    .put("ant", new DetachedPlugin("1.431", "1.0"))
    .put("javadoc", new DetachedPlugin("1.431", "1.0"))
    .put("external-monitor-job", new DetachedPlugin("1.468", "1.0"))
    .put("ldap", new DetachedPlugin("1.468", "1.0"))
    .put("pam-auth", new DetachedPlugin("1.468", "1.0"))
    .put("mailer", new DetachedPlugin("1.494", "1.2"))
    .put("matrix-auth", new DetachedPlugin("1.536", "1.0.2"))
    .put("windows-slaves", new DetachedPlugin("1.548", "1.0"))
    .put("antisamy-markup-formatter", new DetachedPlugin("1.554", "1.0"))
    .put("matrix-project", new DetachedPlugin("1.562", "1.0"))
    .put("junit", new DetachedPlugin("1.578", "1.0"))
    .build();

  private class PluginContext {
    private String jenkinsVersion;
    private Map<String, String> bundledPlugins;
    private Map<String, PluginHolder> plugins = new HashMap<>();
    private Map<String, PluginDesc> pluginCache = new HashMap<>();
    private List<String> errors = new ArrayList<>();

    public PluginContext(String jenkinsVersion, Map<String, String> bundledPlugins) {
      this.jenkinsVersion = jenkinsVersion;
      this.bundledPlugins = bundledPlugins;
    }

    public String getJenkinsVersion() {
      return jenkinsVersion;
    }

    void setPlugin(String key, PluginHolder plugin) {
      plugins.put(key, plugin);
    }

    PluginHolder getPlugin(String key) {
      return plugins.get(key);
    }

    String getBundledVersion(String key) {
      return bundledPlugins.get(key);
    }

    void error(String error, Object... args) {
      errors.add(msg(error, args));
    }

    private String msg(String msg, Object... args) {
      if (args == null || args.length == 0) {
        return msg;
      }
      return String.format(msg, args);
    }

    PluginDesc describe(Artifact art) throws RepositoryException, IOException {
      String key = art.getGroupId() + ":" + art.getArtifactId() + ":" + art.getVersion();
      if (pluginCache.containsKey(key)) {
        return pluginCache.get(key);
      }
      PluginDesc p = doDescribe(this, art);
      pluginCache.put(key, p);

      if (p != null) {
        // in case if resolved to a different gav (old groupId)
        String key2 = p.art.getGroupId() + ":" + p.art.getArtifactId() + ":" + p.art.getVersion();
        if (!key.equals(key2)) {
          pluginCache.put(key2, p);
        }
      }
      return p;
    }

  }

  private static class PluginHolder {
    PluginDesc plugin;
    JenkinsPlugin pinned;
    Set<DepHolder> dependencies;
    boolean redundant;

    PluginHolder(PluginDesc plugin, JenkinsPlugin pinned) {
      this.plugin = plugin;
      this.pinned = pinned;
    }

    String getArtifactId() {
      return plugin.art.getArtifactId();
    }

    String getVersion() {
      return plugin.art.getVersion();
    }
  }

  private static class DepHolder {
    final String key;
    final String version;
    final boolean optional;

    DepHolder(String key, String version, boolean optional) {
      this.key = key;
      this.version = version;
      this.optional = optional;
    }
  }

  private static class PluginDesc {
    final String key;
    final Artifact art;
    final List<Dependency> deps;
    final String jenkinsVersion;

    PluginDesc(String key, Artifact art, List<Dependency> deps, String jenkinsVersion) {
      this.key = key;
      this.art = art;
      this.deps = deps;
      this.jenkinsVersion = jenkinsVersion;
    }
  }

  private static class PluginDepDesc {
    final PluginDesc plugin;
    final boolean optional;

    PluginDepDesc(PluginDesc plugin, boolean optional) {
      this.plugin = plugin;
      this.optional = optional;
    }
  }

  public static final class DetachedPlugin {
    final String splitWhen;
    final String requireVersion;
    final String groupId;

    DetachedPlugin(String splitWhen, String requireVersion) {
      this(splitWhen, requireVersion, DEFAULT_GROUP_ID);
    }

    DetachedPlugin(String splitWhen, String requireVersion, String groupId) {
      this.splitWhen = splitWhen;
      this.requireVersion = requireVersion;
      this.groupId = groupId;
    }
  }

  public static class JenkinsPluginsRequest {
    private String jenkinsVersion;
    private File targetDir;
    private List<JenkinsPlugin> plugins;
    private Map<String, String> bundledPlugins;

    public JenkinsPluginsRequest(String jenkinsVersion, File targetDir, List<JenkinsPlugin> plugins, Map<String, String> bundledPlugins) {
      this.jenkinsVersion = jenkinsVersion;
      this.targetDir = targetDir;
      this.plugins = plugins;
      this.bundledPlugins = bundledPlugins;
    }

    public String getJenkinsVersion() {
      return jenkinsVersion;
    }

    public File getTargetDir() {
      return targetDir;
    }

    public List<JenkinsPlugin> getPlugins() {
      return plugins;
    }

    public Map<String, String> getBundledPlugins() {
      return bundledPlugins;
    }
  }

  public static class JenkinsPlugin {
    private String groupId;
    private String artifactId;
    private String version;
    private boolean includeOptional;

    public JenkinsPlugin(String groupId, String artifactId, String version, boolean includeOptional) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
      this.includeOptional = includeOptional;
    }

    public String getGroupId() {
      return groupId;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public String getVersion() {
      return version;
    }

    public boolean isIncludeOptional() {
      return includeOptional;
    }
  }

}
