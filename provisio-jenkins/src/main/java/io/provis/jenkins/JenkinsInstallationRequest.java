package io.provis.jenkins;

import java.io.File;

import io.provis.jenkins.config.Configuration;

public class JenkinsInstallationRequest {

  private final File targetDir;
  private Configuration configuration;
  private File webappOverrides;
  private File configOverrides;

  public JenkinsInstallationRequest(File targetDir, Configuration configuration) {
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

  public JenkinsInstallationRequest webappOverrides(File webappOverrides) {
    this.webappOverrides = webappOverrides;
    return this;
  }

  public JenkinsInstallationRequest configOverrides(File configOverrides) {
    this.configOverrides = configOverrides;
    return this;
  }

}
