package io.provis.model.v2;

import java.util.List;

public class Artifact {

  private String id;
  private List<Action> actions;
  
  public String getId() {
    return id;
  }
  
  public List<Action> getActions() {
    return actions;
  }
}
