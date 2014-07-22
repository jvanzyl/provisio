package io.tesla.proviso.archive.zip;

import io.tesla.proviso.archive.ArchiverTypeTest;
import io.tesla.proviso.archive.ArchiverValidator;

public class ZipArchiverTypeTest extends ArchiverTypeTest {
  protected String getArchiveExtension() {
    return "zip";
  }

  @Override
  protected ArchiverValidator validator() {
    return new ZipArchiveValidator();
  }   
}
