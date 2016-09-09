package io.provis.jenkins;

import java.io.File;

import io.provis.jenkins.config.Configuration;
import io.provis.jenkins.config.MasterConfiguration;

public class JenkinsInstallationContext {
  
  private final File targetDir;
  private Configuration configuration;
  private File webappOverrides;
  private File configOverrides;
  private MasterConfiguration masterConfiguration;
  
  public JenkinsInstallationContext(File targetDir, Configuration configuration) {
    this.targetDir = targetDir;
    this.configuration = configuration;
  }
  
  public String getJenkinsVersion() {
    return configuration.get("jenkins.version");
  }
  
  public File getTargetDir() {
    return targetDir;
  }
  
  public File getWebappOverrides() {
    return webappOverrides;
  }
  
  public File getConfigOverrides() {
    return configOverrides;
  }
  
  public Configuration getConfiguration() {
    return configuration;
  }
  
  public MasterConfiguration getMasterConfiguration() {
    return masterConfiguration;
  }
  
  void setMasterConfiguration(MasterConfiguration masterConfiguration) {
    this.masterConfiguration = masterConfiguration;
  }
  
  public JenkinsInstallationContext webappOverrides(File webappOverrides) {
    this.webappOverrides = webappOverrides;
    return this;
  }
  
  public JenkinsInstallationContext configOverrides(File configOverrides) {
    this.configOverrides = configOverrides;
    return this;
  }
  
}
