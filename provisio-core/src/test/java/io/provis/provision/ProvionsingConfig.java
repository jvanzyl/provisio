package io.provis.provision;

import java.io.File;

public class ProvionsingConfig {
  private File localRepository;
  private String remoteRepositoryUrl;
  
  public ProvionsingConfig(File localRepository, String remoteRepositoryUrl) {
    this.localRepository = localRepository;
    this.remoteRepositoryUrl = remoteRepositoryUrl;
  }

  public File getLocalRepository() {
    return localRepository;
  }

  public String getRemoteRepositoryUrl() {
    return remoteRepositoryUrl;
  }
}
