package ca.vanzyl.provisio.model.action.alter;

import java.util.ArrayList;
import java.util.List;

import ca.vanzyl.provisio.model.ProvisioArtifact;

public class Insert {

  private List<ProvisioArtifact> artifacts = new ArrayList<>();

  public List<ProvisioArtifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<ProvisioArtifact> artifacts) {
    this.artifacts = artifacts;
  }
}
