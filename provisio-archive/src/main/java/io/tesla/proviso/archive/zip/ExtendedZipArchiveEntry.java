package io.tesla.proviso.archive.zip;

import io.tesla.proviso.archive.ExtendedArchiveEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

public class ExtendedZipArchiveEntry extends ZipArchiveEntry implements ExtendedArchiveEntry {
  public ExtendedZipArchiveEntry(String name) {
    super(name); 
  }  
}
