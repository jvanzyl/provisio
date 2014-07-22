package io.tesla.proviso.archive.tar;

import io.tesla.proviso.archive.ExtendedArchiveEntry;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

public class ExtendedTarArchiveEntry extends TarArchiveEntry implements ExtendedArchiveEntry {
  public ExtendedTarArchiveEntry(String name) {
    super(name);
  }
}
