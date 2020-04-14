package ca.vanzyl.provisio;

import ca.vanzyl.provisio.model.ProvisioArtifact;

public class ProvisioUtils {

  public static String coordinateToPath(ProvisioArtifact a) {

    StringBuffer path = new StringBuffer()
        .append(a.getArtifactId())
        .append("-")
        .append(a.getVersion());

    if (a.getClassifier() != null && !a.getClassifier().isEmpty()) {
      path.append("-")
          .append(a.getClassifier());
    }

    path.append(".")
        .append(a.getExtension());

    return path.toString();
  }
}
