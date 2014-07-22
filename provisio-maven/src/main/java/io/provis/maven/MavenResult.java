package io.provis.maven;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class MavenResult {

  private String output;
  private Map<String, List<String>> executedGoals;
  private Collection<String> resolvedArtifacts;
  private Collection<String> deployedArtifacts;
  private List<Throwable> errors;

  public String getOutput() {
    return (output != null) ? output : "";
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public Map<String, List<String>> getExecutedGoals() {
    if (executedGoals == null) {
      executedGoals = new HashMap<String, List<String>>();
    }
    return executedGoals;
  }

  public void setExecutedGoals(Map<String, List<String>> executedGoals) {
    this.executedGoals = executedGoals;
  }

  public Collection<String> getResolvedArtifacts() {
    if (resolvedArtifacts == null) {
      resolvedArtifacts = new LinkedHashSet<String>();
    }
    return resolvedArtifacts;
  }

  public void setResolvedArtifacts(Collection<String> resolvedArtifacts) {
    this.resolvedArtifacts = resolvedArtifacts;
  }

  public Collection<String> getDeployedArtifacts() {
    if (deployedArtifacts == null) {
      deployedArtifacts = new LinkedHashSet<String>();
    }
    return deployedArtifacts;
  }

  public void setDeployedArtifacts(Collection<String> deployedArtifacts) {
    this.deployedArtifacts = deployedArtifacts;
  }

  public List<Throwable> getErrors() {
    if (errors == null) {
      errors = new ArrayList<Throwable>();
    }
    return errors;
  }

  public void setErrors(List<Throwable> errors) {
    this.errors = errors;
  }

  public boolean hasErrors() {
    return errors != null;
  }
  
}
