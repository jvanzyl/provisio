package io.provis.jenkins;

import java.io.File;
import java.util.Map;

import io.provis.jenkins.config.Configuration;

public class JenkinsInstallationRequest {

  private final File target;
  private Configuration configuration;
  private boolean writeMasterKey;

  private File webappOverrides;
  private File configOverrides;
  private Map<String, String> managedVersions;

  public JenkinsInstallationRequest(File target, Configuration configuration) {
    this.target = target;
    this.configuration = configuration;
  }

  public String getJenkinsVersion() {
    return configuration.get("jenkins.version");
  }

  public boolean isWriteMasterKey() {
    return writeMasterKey;
  }

  public File getTarget() {
    return target;
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

  public JenkinsInstallationRequest writeMasterKey(boolean writeMasterKey) {
    this.writeMasterKey = writeMasterKey;
    return this;
  }

  public Map<String, String> getManagedVersions() {
    return managedVersions;
  }

  public JenkinsInstallationRequest webappOverrides(File webappOverrides) {
    this.webappOverrides = webappOverrides;
    return this;
  }

  public JenkinsInstallationRequest configOverrides(File configOverrides) {
    this.configOverrides = configOverrides;
    return this;
  }

  public JenkinsInstallationRequest managedVersions(Map<String, String> managedVersions) {
    this.managedVersions = managedVersions;
    return this;
  }

}
