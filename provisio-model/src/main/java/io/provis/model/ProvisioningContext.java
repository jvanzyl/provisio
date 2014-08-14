package io.provis.model;

import java.util.Map;

public class ProvisioningContext {   

  private Map<String,String> variables;

  public Map<String, String> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, String> variables) {
    this.variables = variables;
  }
}
