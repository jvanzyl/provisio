package ca.vanzyl.provisio.model.action.alter;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import java.util.ArrayList;
import java.util.List;

public class Insert {

    private List<ProvisioArtifact> artifacts = new ArrayList<>();

    public List<ProvisioArtifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<ProvisioArtifact> artifacts) {
        this.artifacts = artifacts;
    }
}
