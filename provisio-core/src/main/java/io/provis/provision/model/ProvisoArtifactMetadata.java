package io.provis.provision.model;

import io.provis.model.Action;

import java.util.List;

public class ProvisoArtifactMetadata {
  
  private String directory;
  private List<Action> artifactActions;
  private List<Action> directoryActions;
  
  public String getDirectory() {
    return directory;
  }
  
  public void setDirectory(String directory) {
    this.directory = directory;
  }
  
  public List<Action> getArtifactActions() {
    return artifactActions;
  }
  
  public void setArtifactActions(List<Action> artifactActions) {
    this.artifactActions = artifactActions;
  }
  
  public List<Action> getDirectoryActions() {
    return directoryActions;
  }
  
  public void setDirectoryActions(List<Action> directoryActions) {
    this.directoryActions = directoryActions;
  }
}
