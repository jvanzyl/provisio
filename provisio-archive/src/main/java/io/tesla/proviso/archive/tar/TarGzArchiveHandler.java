package io.tesla.proviso.archive.tar;

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
  public ExtendedArchiveEntry createEntryFor(String entryName, Entry archiveEntry, boolean isExecutable) {
    ExtendedTarArchiveEntry entry = new ExtendedTarArchiveEntry(entryName);
    entry.setSize(archiveEntry.getSize());
    if (isExecutable) {
      entry.setMode(FileMode.EXECUTABLE_FILE.getBits());
    }     
    if(archiveEntry.getFileMode() != -1) {
      entry.setMode(archiveEntry.getFileMode());
    }
    return entry;
  }
}