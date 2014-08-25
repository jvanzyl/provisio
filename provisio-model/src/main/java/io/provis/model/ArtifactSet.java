package io.provis.model;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

public class ArtifactSet {

  // parse time
  private String directory;
  private String reference;

  private List<ProvisioArtifact> artifacts;
  private List<Resource> resources;
  // children
  private List<ArtifactSet> artifactSets;
  private List<String> excludes;

  // runtime
  private ArtifactSet parent;
  private File outputDirectory;
  private Map<String, ProvisioArtifact> artifactMap;
  private Set<ProvisioArtifact> resolvedArtifacts;

  public String getDirectory() {
    return directory;
  }

  public String getReference() {
    return reference;
  }

  public void addArtifact(ProvisioArtifact artifact) {
    if (artifacts == null) {
      artifacts = Lists.newArrayList();
    }
    artifacts.add(artifact);
  }

  public List<ProvisioArtifact> getArtifacts() {
    return artifacts;
  }

  public List<Resource> getResources() {
    return resources;
  }

  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
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

  public Map<String, ProvisioArtifact> getArtifactMap() {
    if (artifactMap == null) {
      artifactMap = new LinkedHashMap<String, ProvisioArtifact>();
    }
    if (artifacts != null) {
      for (ProvisioArtifact artifact : artifacts) {
        artifactMap.put(artifact.getCoordinate(), artifact);
      }
    }
    return artifactMap;
  }

  @Override
  public String toString() {
    return "ArtifactSet [directory=" + directory + ", artifacts=" + artifacts + "]";
  }

  //
  // In order to set the parent references we use this technique: http://xstream.codehaus.org/faq.html#Serialization_initialize_transient
  //
  private Object readResolve() {
    if (artifactSets != null) {
      for (ArtifactSet child : artifactSets) {
        child.setParent(this);
      }
    }
    return this;
  }

}
