package io.provis.jenkins;

import java.io.File;

import io.provis.jenkins.config.MasterConfiguration;

public class JenkinsInstallationResponse {
  private File installDir;
  private File workDir;
  private MasterConfiguration configuration;

  public JenkinsInstallationResponse(File installDir, File workDir, MasterConfiguration configuration) {
    this.installDir = installDir;
    this.workDir = workDir;
    this.configuration = configuration;
  }

  public File getInstallDir() {
    return installDir;
  }

  public File getWorkDir() {
    return workDir;
  }

  public MasterConfiguration getConfiguration() {
    return configuration;
  }
}
