package io.provis.provision.model;

import io.provis.model.ProvisioningAction;

import java.util.List;

public class ProvisoArtifactMetadata {
  
  private String directory;
  private List<ProvisioningAction> artifactActions;
  private List<ProvisioningAction> directoryActions;
  
  public String getDirectory() {
    return directory;
  }
  
  public void setDirectory(String directory) {
    this.directory = directory;
  }
  
  public List<ProvisioningAction> getArtifactActions() {
    return artifactActions;
  }
  
  public void setArtifactActions(List<ProvisioningAction> artifactActions) {
    this.artifactActions = artifactActions;
  }
  
  public List<ProvisioningAction> getDirectoryActions() {
    return directoryActions;
  }
  
  public void setDirectoryActions(List<ProvisioningAction> directoryActions) {
    this.directoryActions = directoryActions;
  }
}
