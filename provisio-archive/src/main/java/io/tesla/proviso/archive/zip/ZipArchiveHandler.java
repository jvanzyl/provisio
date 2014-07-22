package io.tesla.proviso.archive.zip;

import io.tesla.proviso.archive.ArchiveHandler;
import io.tesla.proviso.archive.Entry;
import io.tesla.proviso.archive.ExtendedArchiveEntry;
import io.tesla.proviso.archive.FileMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

public class ZipArchiveHandler implements ArchiveHandler {

  private final File archive;

  public ZipArchiveHandler(File archive) {
    this.archive = archive;
  }

  @Override
  public ArchiveInputStream getInputStream() throws IOException {
    return new ZipArchiveInputStream(new FileInputStream(archive));
  }

  @Override
  public ArchiveOutputStream getOutputStream() throws IOException {
    return new ZipArchiveOutputStream(new FileOutputStream(archive));
  }

  @Override
  public ExtendedArchiveEntry createEntryFor(String entryName, Entry entry, boolean isExecutable) {
    ExtendedZipArchiveEntry archiveEntry = new ExtendedZipArchiveEntry(entryName);
    archiveEntry.setSize(entry.getSize());
    if (isExecutable) {
      archiveEntry.setUnixMode(FileMode.EXECUTABLE_FILE.getBits());
    }
    if(entry.getFileMode() != -1) {
      archiveEntry.setUnixMode(entry.getFileMode());
    }
    
    return archiveEntry;
  }
}