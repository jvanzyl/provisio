package io.tesla.proviso.archive.tar;

import io.tesla.proviso.archive.ArchiverTypeTest;
import io.tesla.proviso.archive.ArchiverValidator;

public class TarGzArchiverTypeTest extends ArchiverTypeTest {
  protected String getArchiveExtension() {
    return "tar.gz";
  }

  @Override
  protected ArchiverValidator validator() {
    return new TarGzArchiveValidator();
  }
}
