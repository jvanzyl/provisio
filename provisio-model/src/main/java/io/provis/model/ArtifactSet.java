package io.provis.model;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

public class ArtifactSet {

  private ArtifactSet parent;

  private String directory;
  private String description;
  private Map<String, ProvisioArtifact> artifactMap;
  private Map<String, ProvisioningAction> actionMap;
  private Collection<String> excludes;
  private Map<String, ProvisioArtifact> resolvedArtifacts;
  private Map<String, ProvisioArtifact> artifactMapKeyedByGA;
  private File outputDirectory;

  public ArtifactSet(String directory, List<ProvisioArtifact> artifacts, List<ProvisioningAction> actions, File outputDirectory) {
    this.directory = directory;
    this.outputDirectory = outputDirectory;
    //
    // Artifacts
    //
    artifactMap = new LinkedHashMap<String, ProvisioArtifact>();
    for (ProvisioArtifact artifact : artifacts) {
      artifactMap.put(artifact.getCoordinate(), artifact);
    }
    //
    // Actions
    //
    actionMap = new LinkedHashMap<String, ProvisioningAction>();
    for (ProvisioningAction action : actions) {
      Named javaxNamed = action.getClass().getAnnotation(Named.class);
      actionMap.put(javaxNamed.value(), action);
    }
  }

  public ProvisioArtifact artifact(String coordinate) {
    return artifactMap.get(coordinate);
  }

  public ProvisioningAction action(String name) {
    return actionMap.get(name);
  }

  public Collection<ProvisioningAction> getActions() {
    return actionMap.values();
  }

  public List<ArtifactSet> getFileSets() {
    return null;
  }

  public void setActualOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public File getActualOutputDirectory() {
    return outputDirectory;
  }

  public ArtifactSet getParent() {
    return parent;
  }

  public void setParent(ArtifactSet parent) {
    this.parent = parent;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Map<String, ProvisioArtifact> getArtifactMap() {
    return artifactMap;
  }

  public void setArtifactMap(Map<String, ProvisioArtifact> artifactMap) {
    this.artifactMap = artifactMap;
  }

  public Map<String, ProvisioningAction> getActionMap() {
    return actionMap;
  }

  public void setActionMap(Map<String, ProvisioningAction> actionMap) {
    this.actionMap = actionMap;
  }

  public Collection<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(Collection<String> excludes) {
    this.excludes = excludes;
  }

  public Map<String, ProvisioArtifact> getResolvedArtifacts() {
    return resolvedArtifacts;
  }

  public void setResolvedArtifacts(Map<String, ProvisioArtifact> resolvedArtifacts) {
    this.resolvedArtifacts = resolvedArtifacts;
  }

  public Map<String, ProvisioArtifact> getArtifactMapKeyedByGA() {
    if(artifactMapKeyedByGA == null) {
      
    }
    return artifactMapKeyedByGA;
  }

  public void setArtifactMapKeyedByGA(Map<String, ProvisioArtifact> artifactMapKeyedByGA) {
    this.artifactMapKeyedByGA = artifactMapKeyedByGA;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }
  
  
  
}
