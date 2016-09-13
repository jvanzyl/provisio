/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class ArtifactSet {

  // parse time
  private String directory;
  private String reference;
  private String from;

  private List<ProvisioArtifact> artifacts = Lists.newArrayList();
  private List<Resource> resources = Lists.newArrayList();
  // children
  private List<ArtifactSet> artifactSets = Lists.newArrayList();
  private List<Exclusion> exclusions;

  // runtime
  private ArtifactSet parent;
  private File outputDirectory;
  private Map<String, ProvisioArtifact> artifactMap = Maps.newLinkedHashMap();
  private Set<ProvisioArtifact> resolvedArtifacts = Sets.newHashSet();

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public void addArtifact(ProvisioArtifact artifact) {
    artifacts.add(artifact);
  }

  public List<ProvisioArtifact> getArtifacts() {
    return artifacts;
  }

  public void addResource(Resource resource) {
    resources.add(resource);
  }

  public List<Resource> getResources() {
    return resources;
  }

  public void addArtifactSet(ArtifactSet artifactSet) {
    artifactSets.add(artifactSet);
  }

  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }

  // runtime

  public List<Exclusion> getExcludes() {
    return exclusions;
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
    if (artifacts != null) {
      for (ProvisioArtifact artifact : artifacts) {
        artifactMap.put(artifact.getCoordinate(), artifact);
      }
    }
    return artifactMap;
  }

  @Override
  public String toString() {
    return "ArtifactSet [directory=" + directory + ", artifacts=" + artifacts + ", artifactSets=" + artifactSets + ", parent=" + parent + ", resolvedArtifacts=" + resolvedArtifacts + "]";
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
