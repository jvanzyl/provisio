package io.provis.model.v2;

import io.provis.model.ProvisioArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

public class ArtifactSet {
  
  // parse time
  private String directory;
  private List<Artifact> artifacts;
  // children
  private List<ArtifactSet> artifactSets;
  private List<Action> actions;
  private List<String> excludes;
   
  // runtime
  private ArtifactSet parent;
  private File outputDirectory;
  private Map<String, ProvisioArtifact> artifactMap;  
  private Set<ProvisioArtifact> resolvedArtifacts;
    
  public String getDirectory() {
    return directory;
  }
  
  public List<Artifact> getArtifacts() {
    return artifacts;
  }

  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }

  public List<Action> getActions() {
    if(actions == null) {
      actions = Lists.newArrayList();
    }
    return actions;
  }

  // runtime
  
  public List<String> getExcludes() {
    return excludes;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }
  
  // maybe we can do this in the model
  
  public ArtifactSet getParent() {
    return parent;
  }

  public void setParent(ArtifactSet parent) {
    this.parent = parent;
  }

  public Set<ProvisioArtifact> getResolvedArtifacts() {
    return resolvedArtifacts;
  }

  public void setResolvedArtifacts(Set<ProvisioArtifact> resolvedArtifacts) {
    this.resolvedArtifacts = resolvedArtifacts;
  }

  Lookup lookup = new Lookup();
  
  private List<ProvisioArtifact> runtimeArtifacts() {
    List<ProvisioArtifact> runtimeArtifacts = new ArrayList<ProvisioArtifact>();
    for(Artifact a : artifacts) {
      ProvisioArtifact pa = new ProvisioArtifact(a.getId(), a);
      runtimeArtifacts.add(pa);
    }
    return runtimeArtifacts;
  }
  
  public Map<String, ProvisioArtifact> getArtifactMap() {
    if(artifactMap == null) {
      artifactMap = new LinkedHashMap<String, ProvisioArtifact>();
      for (ProvisioArtifact artifact : runtimeArtifacts()) {
        artifactMap.put(artifact.getCoordinate(), artifact);
      }      
    }
    return artifactMap;
  }
}
