/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Runtime {

  private String id;
  // Runtime level actions
  private List<ProvisioningAction> actions;
  // ArtifactSets
  private List<ArtifactSet> artifactSets;
  // ArtifactSet references
  private Map<String, ArtifactSet> artifactSetReferences;
  // Artifact references
  private Map<String, ProvisioArtifact> artifactReferences;
  // ResourceSets
  private List<ResourceSet> resourceSets;
  // Variables
  Map<String, String> variables;
  // FileSets
  private List<FileSet> fileSets;

  public Runtime() {
    this.actions = Lists.newArrayList();
    this.artifactSets = Lists.newArrayList();
    this.artifactSetReferences = Maps.newHashMap();
    this.artifactReferences = Maps.newHashMap();
    this.resourceSets = Lists.newArrayList();
    this.fileSets = Lists.newArrayList();
  }

  public String getId() {
    return id;
  }

  public List<ProvisioningAction> getActions() {
    return actions;
  }

  public void addAction(ProvisioningAction action) {
    actions.add(action);
  }

  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }

  public void addArtifactSet(ArtifactSet artifactSet) {
    artifactSets.add(artifactSet);
  }

  public Map<String, ArtifactSet> getArtifactSetReferences() {
    return artifactSetReferences;
  }

  public void addArtifactSetReference(String refId, ArtifactSet artifactSet) {
    artifactSetReferences.put(refId, artifactSet);
  }

  public Map<String, ProvisioArtifact> getArtifactReferences() {
    return artifactReferences;
  }

  public void addArtifactReference(String refId, ProvisioArtifact artifact) {
    artifactReferences.put(refId, artifact);
  }

  public List<ResourceSet> getResourceSets() {
    return resourceSets;
  }

  public void addResourceSet(ResourceSet resourceSet) {
    resourceSets.add(resourceSet);
  }

  public List<FileSet> getFileSets() {
    return fileSets;
  }

  public void addFileSet(FileSet fileSet) {
    fileSets.add(fileSet);
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, String> variables) {
    this.variables = variables;
  }

  public Set<String> getGAsOfArtifacts() {
    Set<String> dependenciesInVersionlessForm = new HashSet<String>();
    for (ArtifactSet artifactSet : artifactSets) {
      if (artifactSet.getArtifacts() != null) {
        for (ProvisioArtifact artifact : artifactSet.getArtifacts()) {
          if (artifact.getReference() == null) {
            dependenciesInVersionlessForm.add(artifact.getGA());
          }
        }
      }
    }
    return dependenciesInVersionlessForm;
  }

  public Set<String> getVersionlessCoordinatesOfArtifacts() {
    Set<String> dependenciesInVersionlessForm = new HashSet<String>();
    for (ArtifactSet artifactSet : artifactSets) {
      if (artifactSet.getArtifacts() != null) {
        for (ProvisioArtifact artifact : artifactSet.getArtifacts()) {
          if(artifact.getReference() == null) {
            dependenciesInVersionlessForm.add(artifact.toVersionlessCoordinate());
          }
        }
      }
    }
    return dependenciesInVersionlessForm;
  }
}
