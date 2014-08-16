package io.provis.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

public class Runtime {

  private String id;
  private List<ProvisioningAction> actions;
  private List<ArtifactSet> artifactSets;

  public String getId() {
    return id;
  }

  public List<ProvisioningAction> getActions() {
    return actions;
  }
  
  public void addAction(ProvisioningAction action) {
    if(actions == null) {
      actions = Lists.newArrayList();
    }
    actions.add(action);
  }
  
  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }

  public Set<String> getGACoordinatesOfArtifacts() {
    Set<String> dependenciesInGAForm = new HashSet<String>();
    for (ArtifactSet artifactSet : artifactSets) {
      for (ProvisioArtifact gaDependency : artifactSet.getArtifactMap().values()) {
        dependenciesInGAForm.add(gaDependency.getGA());
      }
      if (artifactSet.getArtifactSets() != null) {
        for (ArtifactSet childFileSet : artifactSet.getArtifactSets()) {
          for (ProvisioArtifact gaDependency : childFileSet.getArtifactMap().values()) {
            dependenciesInGAForm.add(gaDependency.getGA());
          }
        }
      }
    }
    return dependenciesInGAForm;
  }

  public void addArtifactSet(ArtifactSet artifactSet) {
    if(artifactSets == null) {
      artifactSets = Lists.newArrayList();
    }
    artifactSets.add(artifactSet);
  }
}
