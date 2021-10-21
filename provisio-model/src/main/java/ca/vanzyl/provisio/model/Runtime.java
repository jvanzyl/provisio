/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    this.actions = new ArrayList<>();
    this.artifactSets = new ArrayList<>();
    this.artifactSetReferences = new HashMap<>();
    this.artifactReferences = new HashMap<>();
    this.resourceSets = new ArrayList<>();
    this.fileSets = new ArrayList<>();
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
    Set<String> dependenciesInVersionlessForm = new HashSet<>();
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
    Set<String> dependenciesInVersionlessForm = new HashSet<>();
    for (ArtifactSet artifactSet : artifactSets) {
      if (artifactSet.getArtifacts() != null) {
        for (ProvisioArtifact artifact : artifactSet.getArtifacts()) {
          if (artifact.getReference() == null) {
            dependenciesInVersionlessForm.add(artifact.toVersionlessCoordinate());
          }
        }
      }
    }
    return dependenciesInVersionlessForm;
  }
}
