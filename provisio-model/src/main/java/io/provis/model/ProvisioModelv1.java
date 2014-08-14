package io.provis.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

public class ProvisioModelv1 {

  private String id;
  private String versionMap;
  private Map<String, ArtifactSet> artifactSetMap;
  private Map<String, ProvisioningAction> actionMap;
  
  public ProvisioModelv1(String id, String versionMap, List<ArtifactSet> fileSets, List<ProvisioningAction> actions) {
    this.id = id;
    this.versionMap = versionMap;
    //
    // ArtifactSets
    //
    artifactSetMap = new LinkedHashMap<String, ArtifactSet>();
    for (ArtifactSet fs : fileSets) {
      artifactSetMap.put(fs.getDirectory(), fs);
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

  public String getVersionMap() {
    return versionMap;
  }
  
  //
  // ArtifactSets
  //
  public ArtifactSet artifactSet(String name) {
    return artifactSetMap.get(name);
  }

  public Collection<ArtifactSet> getArtifactSets() {
    return artifactSetMap.values();
  }

  public void addArtifactSet(ArtifactSet artifactSet) {
    artifactSetMap.put(artifactSet.getDirectory(), artifactSet);
  }
  
  //
  // Actions
  //
  public ProvisioningAction action(String name) {
    return actionMap.get(name);
  }
  
  public void addAction(ProvisioningAction action) {
    Named javaxNamed = action.getClass().getAnnotation(Named.class);
    actionMap.put(javaxNamed.value(), action);
  }
  
  public Collection<ProvisioningAction> getActions() {
    return actionMap.values();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  
}
