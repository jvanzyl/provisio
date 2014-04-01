package io.tesla.proviso.archive.tar;

import io.provis.model.ProvisioContext;
import io.tesla.proviso.archive.ArchiveHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

public class TarGzArchiveHandler implements ArchiveHandler {

  private final File archive;

  public TarGzArchiveHandler(File archive) {
    this.archive = archive;
  }

  @Override
  public ArchiveInputStream getInputStream() throws IOException {
    return new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive)));
  }

  @Override
  public ArchiveOutputStream getOutputStream() throws IOException {
    TarArchiveOutputStream aos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(archive)));
    aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
    return aos;
  }

  @Override
  public ArchiveEntry createEntryFor(String archiveEntryName, File file, ProvisioContext context) {
    TarArchiveEntry entry = new TarArchiveEntry(archiveEntryName);
    entry.setSize(file.length());
    if (context != null && context.getFileEntries().get(archiveEntryName) != null) {
      entry.setMode(context.getFileEntries().get(archiveEntryName).getMode());
    }
    return entry;
  }
}