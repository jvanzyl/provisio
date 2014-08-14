package io.provis.model.v2;

import java.util.List;

public class Runtime {

  private String id;
  private List<ArtifactSet> artifactSets;
  
  public String getId() {
    return id;
  }
  
  public List<ArtifactSet> getArtifactSets() {
    return artifactSets;
  }
}
