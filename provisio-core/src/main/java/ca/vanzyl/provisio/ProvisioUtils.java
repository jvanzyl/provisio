package ca.vanzyl.provisio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ca.vanzyl.provisio.model.ProvisioArtifact;

import static java.util.Objects.requireNonNull;

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

  public static long copy(InputStream from, OutputStream to) throws IOException {
    requireNonNull(from);
    requireNonNull(to);
    byte[] buf = new byte[4096];
    long total = 0L;

    while(true) {
      int r = from.read(buf);
      if (r == -1) {
        return total;
      }

      to.write(buf, 0, r);
      total += (long)r;
    }
  }
}
