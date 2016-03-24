package io.provis.provision.action.artifact.alter;

import java.util.List;

import com.google.common.collect.Lists;

import io.provis.model.ProvisioArtifact;

public class Insert {
  
  private List<ProvisioArtifact> artifacts = Lists.newArrayList();  
  
  public List<ProvisioArtifact> getArtifacts() {
    return artifacts;
  }  
}
