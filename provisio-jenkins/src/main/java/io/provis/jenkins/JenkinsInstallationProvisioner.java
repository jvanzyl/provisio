package io.provis.jenkins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderDelegate;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.Actions;
import io.provis.MavenProvisioner;
import io.provis.ProvisioningException;
import io.provis.jenkins.JenkinsPluginsProvisioner.JenkinsPlugin;
import io.provis.jenkins.JenkinsPluginsProvisioner.JenkinsPluginsRequest;
import io.provis.jenkins.aether.Repository;
import io.provis.jenkins.aether.ResolutionSystem;
import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.model.ProvisioningRequest;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;

public class JenkinsInstallationProvisioner {

  private static final Logger log = LoggerFactory.getLogger(JenkinsInstallationProvisioner.class);

  public static final String JENKINS_REPO = "http://repo.jenkins-ci.org/public/";

  private static final String DEFAULT_MAIN_CLASS = "io.provis.jenkins.launch.Jenkins";
  private static final String DEFAULT_PROCESS_NAME = "jenkins";

  private ArtifactDescriptorReader descriptorReader;
  private RuntimeReader reader;

  private RepositorySystem repositorySystem;
  private RepositorySystemSession session;
  private List<RemoteRepository> remoteRepos;

  public static JenkinsInstallationProvisioner create(File localRepository, String... remoteRepositories) {
    ResolutionSystem resolution = new ResolutionSystem(localRepository);
    for (String remoteRepository : remoteRepositories) {
      resolution.remoteRepository(remoteRepository);
    }
    // If no repositories are specified then add the jenkins repository by default
    if (remoteRepositories.length == 0) {
      resolution.remoteRepository(new Repository("jenkins-ci.org", JENKINS_REPO));
    }
    RepositorySystemSession session = newRepositorySystemSession(localRepository);
    return new JenkinsInstallationProvisioner(resolution.repositorySystem(), session, resolution.remoteRepositories(), resolution.getDescriptorReader());
  }

  public static JenkinsInstallationProvisioner create(RepositorySystem repositorySystem, RepositorySystemSession existingSession, List<RemoteRepository> remoteRepos,
    ArtifactDescriptorReader descriptorReader) {
    RepositorySystemSession session = newRepositorySystemSession(existingSession.getLocalRepository().getBasedir());
    return new JenkinsInstallationProvisioner(repositorySystem, session, remoteRepos, descriptorReader);
  }

  public JenkinsInstallationProvisioner(RepositorySystem repositorySystem, RepositorySystemSession session, List<RemoteRepository> remoteRepos, ArtifactDescriptorReader descriptorReader) {
    this.repositorySystem = repositorySystem;
    this.session = session;
    this.remoteRepos = remoteRepos;
    this.descriptorReader = descriptorReader;
    reader = new RuntimeReader(Actions.defaultActionDescriptors());
  }

  public JenkinsInstallationResponse provision(JenkinsInstallationRequest req) throws Exception {
    Configuration conf = req.getConfiguration();

    MavenProvisioner provisioner = createProvisioner(conf);

    Runtime runtime;
    try (InputStream in = getClass().getResourceAsStream("jenkins-provisio.xml")) {
      runtime = reader.read(in, conf);
    }
    File installDir = new File(req.getTarget(), "jenkins-installation");
    File workDir = new File(req.getTarget(), "jenkins-work");

    provisionRuntime(provisioner, req, runtime, installDir);
    MasterConfiguration mc = provisionMasterConfiguration(provisioner, req, workDir);
    updateEtc(req, mc, installDir);

    return new JenkinsInstallationResponse(installDir, workDir, mc);
  }

  private MavenProvisioner createProvisioner(Configuration conf) {
    List<RemoteRepository> repos;
    Configuration reposConf = conf.subset("remoteRepository");
    if (reposConf.isEmpty()) {
      repos = remoteRepos;
    } else {
      repos = new ArrayList<>();
      for (Map.Entry<String, String> e : reposConf.entrySet()) {
        repos.add(ResolutionSystem.createRepository(new Repository(e.getKey(), e.getValue())));
      }
      repos.addAll(remoteRepos);
    }

    return new MavenProvisioner(repositorySystem, session, repos);
  }

  private void provisionRuntime(MavenProvisioner provisioner, JenkinsInstallationRequest req, Runtime runtime, File dir) throws Exception {

    log.info("Provisioning jenkins runtime v" + req.getJenkinsVersion());
    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(dir);
    request.setRuntimeDescriptor(runtime);
    Map<String, String> vars = new HashMap<>(req.getConfiguration());
    if (!vars.containsKey("main-class")) {
      vars.put("main-class", DEFAULT_MAIN_CLASS);
    }
    if (!vars.containsKey("process-name")) {
      vars.put("process-name", DEFAULT_PROCESS_NAME);
    }
    request.setVariables(vars);
    provisioner.provision(request);

    log.info("Provisioning plugins");
    provisionPlugins(provisioner, req, dir);
  }

  private void provisionPlugins(MavenProvisioner provisioner, JenkinsInstallationRequest req, File installDir) throws Exception {

    Configuration pluginsConf = req.getConfiguration().subset("jenkins.plugins");

    List<JenkinsPlugin> plugins = new ArrayList<>();

    for (Map.Entry<String, Configuration> e : pluginsConf.partition().entrySet()) {
      String key = e.getKey();
      Configuration conf = e.getValue();
      String v = conf.get("");

      if (v != null) {

        boolean includeOptional = conf.getBool("includeOptional");

        // jenkins.plugins.<artifactId> = <version>
        // jenkins.plugins.<bla> = <artifactId>:<version>
        // jenkins.plugins.<bla> = <groupId>:<artifactId>:<version>
        JenkinsPlugin plugin;
        int c = v.indexOf(':');
        if (c == -1) {
          plugin = new JenkinsPlugin(JenkinsPluginsProvisioner.DEFAULT_GROUP_ID, key, v, includeOptional);
        } else {
          int cc = v.indexOf(':', c + 1);
          if (cc == -1) {
            plugin = new JenkinsPlugin(JenkinsPluginsProvisioner.DEFAULT_GROUP_ID, v.substring(0, c), v.substring(c + 1), includeOptional);
          } else {
            plugin = new JenkinsPlugin(v.substring(0, c), v.substring(c + 1, cc), v.substring(cc + 1), includeOptional);
          }
        }
        plugins.add(plugin);
      }
    }

    Map<String, String> bundledPlugins = collectBundledPlugins(new File(installDir, "jenkins/WEB-INF"));

    if (!plugins.isEmpty()) {
      File output = new File(installDir, "plugins");
      JenkinsPluginsProvisioner pp = new JenkinsPluginsProvisioner(provisioner, descriptorReader);
      try {
        pp.provision(new JenkinsPluginsRequest(req.getJenkinsVersion(), output, plugins, bundledPlugins));
      } catch (RepositoryException e) {
        throw new ProvisioningException("Unable to provision jenkins plugins", e);
      }
    }

  }

  private Map<String, String> collectBundledPlugins(File webinf) throws IOException {

    File pluginsDir = new File(webinf, "detached-plugins");
    if (!pluginsDir.isDirectory()) {
      pluginsDir = new File(webinf, "plugins");
    }

    if (!pluginsDir.isDirectory()) {
      return Collections.emptyMap();
    }

    Map<String, String> result = new HashMap<>();
    for (File f : pluginsDir.listFiles()) {
      if (f.isFile() && (f.getName().endsWith(".hpi") || f.getName().endsWith(".jpi"))) {
        Attributes attrs;
        try (JarFile jf = new JarFile(f)) {
          attrs = jf.getManifest().getMainAttributes();
        }

        String key = attrs.getValue("Short-Name");
        String value = attrs.getValue("Plugin-Version");
        result.put(key, value);
      }
    }
    return result;
  }

  private MasterConfiguration provisionMasterConfiguration(MavenProvisioner provisioner, JenkinsInstallationRequest req, File dir) throws IOException {
    log.info("Provisioning configuration");

    File localRepo = provisioner.getRepositorySystemSession().getLocalRepository().getBasedir();
    String remoteRepoUrl;
    if (provisioner.getRemoteRepositories().isEmpty()) {
      remoteRepoUrl = JenkinsConfigurationProvisioner.DEFAULT_REMOTE_REPO;
    } else {
      RemoteRepository remoteRepo = provisioner.getRemoteRepositories().get(0);
      remoteRepoUrl = remoteRepo.getUrl();
    }

    JenkinsConfigurationProvisioner cp = new JenkinsConfigurationProvisioner(localRepo, remoteRepoUrl);
    return cp.provision(req.getConfiguration(), req.getConfigOverrides(), dir);
  }

  private void updateEtc(JenkinsInstallationRequest req, MasterConfiguration configuration, File dir) throws IOException {
    File etcDir = new File(dir, "etc");
    File config = new File(etcDir, "config.properties");
    File jvmConfig = new File(etcDir, "jvm.config");

    {
      Properties props = new Properties();
      String conf = FileUtils.fileRead(config);
      props.load(new StringReader(conf));
      props.setProperty("jenkins.http.port", String.valueOf(configuration.getPort()));

      StringWriter sw = new StringWriter();
      props.store(sw, null);
      FileUtils.fileWrite(config, sw.toString());
    }

    {
      Configuration system = req.getConfiguration().subset("system");
      if (!system.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append(FileUtils.fileRead(jvmConfig));

        for (String v : system.values()) {
          sb.append(v).append("\n");
        }
        FileUtils.fileWrite(jvmConfig, sb.toString());
      }
    }
  }

  private static RepositorySystemSession newRepositorySystemSession(File localRepo) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    // We are not concerned with checking the _remote.repositories files
    try {
      session.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(session, new LocalRepository(localRepo)));
    } catch (NoLocalRepositoryManagerException e) {
      // This should never happen
      throw new IllegalStateException(e);
    }
    // Don't follow remote repositories in POMs
    session.setIgnoreArtifactDescriptorRepositories(true);
    session.setTransferListener(new QuietTransferListener());
    session.setRepositoryListener(new QuietRepositoryListener());
    // resolve pom packaging
    session.setConfigProperty(ArtifactDescriptorReaderDelegate.class.getName(), new ArtifactDescriptorReaderDelegate() {
      @Override
      public void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        super.populateResult(session, result, model);
        result.getProperties().put("packaging", model.getPackaging());
      }
    });
    return session;
  }

  // We're no looking to watch any output here but if we do, in fact, need to watch anything
  // we can make simple changes to these no-op implementations
  private static class QuietRepositoryListener extends AbstractRepositoryListener {
  }

  private static class QuietTransferListener extends AbstractTransferListener {
  }

}
