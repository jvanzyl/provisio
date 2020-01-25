package ca.vanzyl.provisio.model.action.alter;

import java.util.List;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import com.google.common.collect.Lists;

public class Insert {

  private List<ProvisioArtifact> artifacts = Lists.newArrayList();

  public List<ProvisioArtifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<ProvisioArtifact> artifacts) {
    this.artifacts = artifacts;
  }
}
