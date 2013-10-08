package io.provis.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

public class ProvisioModel {

  private String id;
  private String versionMap;
  private Map<String, ArtifactSet> artifactSetMap;
  private Map<String, Action> actionMap;

  public ProvisioModel(String id, String versionMap, List<ArtifactSet> fileSets, List<Action> actions) {
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
    actionMap = new LinkedHashMap<String, Action>();
    for (Action action : actions) {
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
  public Action action(String name) {
    return actionMap.get(name);
  }
  
  public void addAction(Action action) {
    Named javaxNamed = action.getClass().getAnnotation(Named.class);
    actionMap.put(javaxNamed.value(), action);
  }
  
  public Collection<Action> getActions() {
    return actionMap.values();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
  
  
}
