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
  // ResourceSets
  private List<ResourceSet> resourceSets;

  public String getId() {
    return id;
  }

  public List<ProvisioningAction> getActions() {
    return actions;
  }

  public void addAction(ProvisioningAction action) {
    if (actions == null) {
      actions = Lists.newArrayList();
    }
    actions.add(action);
  }

  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }

  public void addArtifactSetReference(String refId, ArtifactSet artifactSet) {
    if (artifactSetReferences == null) {
      artifactSetReferences = Maps.newHashMap();
    }
    artifactSetReferences.put(refId, artifactSet);
  }

  public Map<String, ArtifactSet> getArtifactSetReferences() {
    return artifactSetReferences;
  }

  public List<ResourceSet> getResourceSets() {
    return resourceSets;
  }

  public Set<String> getVersionlessCoordinatesOfArtifacts() {
    Set<String> dependenciesInVersionlessForm = new HashSet<String>();
    for (ArtifactSet artifactSet : artifactSets) {
      if (artifactSet.getArtifacts() != null) {
        for (ProvisioArtifact artifact : artifactSet.getArtifacts()) {
          dependenciesInVersionlessForm.add(artifact.toVersionlessCoordinate());
        }
      }
    }
    return dependenciesInVersionlessForm;
  }

  public void addArtifactSet(ArtifactSet artifactSet) {
    if (artifactSets == null) {
      artifactSets = Lists.newArrayList();
    }
    artifactSets.add(artifactSet);
  }

  public void addResourceSet(ResourceSet resourceSet) {
    if (resourceSets == null) {
      resourceSets = Lists.newArrayList();
    }
    resourceSets.add(resourceSet);
  }
}
