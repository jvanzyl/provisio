package io.provis.jenkins;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositoryException;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.Actions;
import io.provis.MavenProvisioner;
import io.provis.ProvisioningException;
import io.provis.SimpleProvisioner;
import io.provis.jenkins.JenkinsPluginsProvisioner.JenkinsPlugin;
import io.provis.jenkins.JenkinsPluginsProvisioner.JenkinsPluginsRequest;
import io.provis.jenkins.aether.Repository;
import io.provis.jenkins.aether.ResolutionSystem;
import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.model.ProvisioningRequest;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;

public class JenkinsInstallationProvisioner extends SimpleProvisioner {

  private static final Logger log = LoggerFactory.getLogger(JenkinsInstallationProvisioner.class);

  public static final String JENKINS_REPO = "http://repo.jenkins-ci.org/public/";

  private static final String DEFAULT_MAIN_CLASS = "io.provis.jenkins.launch.Jenkins";
  private static final String DEFAULT_PROCESS_NAME = "jenkins";

  private RuntimeReader reader;

  private ResolutionSystem resolution;
  private RepositorySystemSession session;
  private MavenProvisioner provisioner;

  public JenkinsInstallationProvisioner() {
    this(DEFAULT_LOCAL_REPO, DEFAULT_REMOTE_REPO);
  }

  public JenkinsInstallationProvisioner(File localRepository) {
    this(localRepository, DEFAULT_REMOTE_REPO);
  }

  public JenkinsInstallationProvisioner(File localRepository, String remoteRepository) {
    super(localRepository, DEFAULT_REMOTE_REPO);
    reader = new RuntimeReader(Actions.defaultActionDescriptors());

    resolution = new ResolutionSystem(localRepository);
    resolution.remoteRepository(remoteRepositoryUrl);
    if (!remoteRepositoryUrl.equals(JENKINS_REPO)) {
      resolution.remoteRepository(new Repository("jenkins", JENKINS_REPO));
    }
    session = resolution.repositorySystemSession();
    provisioner = new MavenProvisioner(resolution.repositorySystem(), session, resolution.remoteRepositories());
  }

  public void provision(JenkinsInstallationContext ctx) throws Exception {
    Configuration conf = ctx.getConfiguration();
    Runtime runtime;
    try (InputStream in = getClass().getResourceAsStream("jenkins-provisio.xml")) {
      runtime = reader.read(in, conf);
    }
    File installDir = new File(ctx.getTargetDir(), "jenkins-installation");
    File configDir = new File(ctx.getTargetDir(), "jenkins-work");

    provisionRuntime(ctx, runtime, installDir);
    provisionMasterConfiguration(ctx, configDir);
    updateEtc(ctx, ctx.getMasterConfiguration(), installDir);
  }

  private void provisionRuntime(JenkinsInstallationContext ctx, Runtime runtime, File dir) throws Exception {

    log.info("Provisioning jenkins runtime v" + ctx.getJenkinsVersion());
    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(dir);
    request.setRuntimeDescriptor(runtime);
    Map<String, String> vars = new HashMap<>(ctx.getConfiguration());
    if (!vars.containsKey("main-class")) {
      vars.put("main-class", DEFAULT_MAIN_CLASS);
    }
    if (!vars.containsKey("process-name")) {
      vars.put("process-name", DEFAULT_PROCESS_NAME);
    }
    request.setVariables(vars);
    provisioner.provision(request);

    log.info("Provisioning plugins");
    provisionPlugins(ctx, dir);
  }

  private void provisionPlugins(JenkinsInstallationContext ctx, File installDir) throws Exception {

    Configuration pluginsConf = ctx.getConfiguration().subset("jenkins.plugins");

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

    if (!plugins.isEmpty()) {
      File output = new File(installDir, "plugins");
      JenkinsPluginsProvisioner pp = new JenkinsPluginsProvisioner(resolution, session);
      try {
        pp.provision(new JenkinsPluginsRequest(ctx.getJenkinsVersion(), output, plugins));
      } catch (RepositoryException e) {
        throw new ProvisioningException("Unable to provision jenkins plugins", e);
      }
    }

  }

  private void provisionMasterConfiguration(JenkinsInstallationContext ctx, File dir) throws IOException {
    log.info("Provisioning configuration");
    JenkinsConfigurationProvisioner cp = new JenkinsConfigurationProvisioner(localRepository, remoteRepositoryUrl);
    MasterConfiguration mc = cp.provision(ctx.getConfiguration(), ctx.getConfigOverrides(), dir);
    ctx.setMasterConfiguration(mc);
  }

  private void updateEtc(JenkinsInstallationContext ctx, MasterConfiguration configuration, File dir) throws IOException {
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
      Configuration system = ctx.getConfiguration().subset("system");
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

}
