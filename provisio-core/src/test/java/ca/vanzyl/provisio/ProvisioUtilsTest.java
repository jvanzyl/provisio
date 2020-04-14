package ca.vanzyl.provisio;

import static ca.vanzyl.provisio.ProvisioUtils.coordinateToPath;
import static org.junit.Assert.assertEquals;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import org.junit.Test;

public class ProvisioUtilsTest {

  @Test
  public void validateCoordinateToPath() {

    ProvisioArtifact artifact = new ProvisioArtifact("io.prestosql:presto-main:332");
    String path = coordinateToPath(artifact);
    assertEquals("presto-main-332.jar", path);
  }

}
