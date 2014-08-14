package io.provis.model.v2;

import java.util.Map;

public class Action {

  private String id;
  private Map<String,String> parameters;
  
  public String getId() {
    return id;
  }

  public Map<String, String> getParameters() {
    return parameters;
  }
}
