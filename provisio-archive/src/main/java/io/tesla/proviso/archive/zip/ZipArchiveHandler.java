package io.tesla.proviso.archive.zip;

import io.provis.model.ProvisioContext;
import io.tesla.proviso.archive.ArchiveHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
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
  public ArchiveEntry createEntryFor(String archiveEntryName, File file, ProvisioContext context) {
    ZipArchiveEntry entry = new ZipArchiveEntry(archiveEntryName);
    entry.setSize(file.length());
    if (context != null && context.getFileEntries().get(archiveEntryName) != null) {
      entry.setUnixMode(context.getFileEntries().get(archiveEntryName).getMode());
    }
    return entry;
  }

}