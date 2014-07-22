package io.tesla.proviso.archive.tar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.tesla.proviso.archive.ArchiverValidator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;

public class TarGzArchiveValidator implements ArchiverValidator {

  public void assertNumberOfEntriesInArchive(int expectedEntries, File archive) throws IOException {
    Closer closer = Closer.create();
    try {
      TarArchiveInputStream is = closer.register(new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive))));
      int entries = 0;
      while (null != (is.getNextTarEntry())) {
        entries++;
      }
      String message = String.format("Expected %s entries.", entries);
      assertEquals(message, expectedEntries, entries);
    } finally {
      closer.close();
    }
  }

  public void assertPresenceOfEntryInArchive(File archive, String entryName) throws IOException {
    Closer closer = Closer.create();
    try {
      TarArchiveInputStream is = closer.register(new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive))));
      TarArchiveEntry entry;
      Set<String> entryNames = Sets.newHashSet();
      while (null != (entry = is.getNextTarEntry())) {
        entryNames.add(entry.getName());
      }
      assertTrue(String.format("The entry %s is expected to be present, but it is.", entryName), entryNames.contains(entryName));
    } finally {
      closer.close();
    }
  }

  public void assertAbsenceOfEntryInArchive(File archive, String entryName) throws IOException {
    Closer closer = Closer.create();
    try {
      TarArchiveInputStream is = closer.register(new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive))));
      TarArchiveEntry entry;
      Set<String> entryNames = Sets.newHashSet();
      while (null != (entry = is.getNextTarEntry())) {
        entryNames.add(entry.getName());
      }
      assertFalse(String.format("The entry %s is not expected to be present, but is not.", entryName), entryNames.contains(entryName));
    } finally {
      closer.close();
    }
  }

  public void assertContentOfEntryInArchive(File archive, String entryName, String expectedEntryContent) throws IOException {
    Closer closer = Closer.create();
    try {
      TarArchiveInputStream is = closer.register(new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive))));
      TarArchiveEntry entry;
      Map<String, String> entries = Maps.newHashMap();
      while (null != (entry = is.getNextTarEntry())) {
        OutputStream outputStream = new ByteArrayOutputStream();
        ByteStreams.copy(is, outputStream);
        entries.put(entry.getName(), outputStream.toString());
      }
      assertTrue(String.format("The entry %s is expected to be present, but is not.", entryName), entries.containsKey(entryName));
      assertEquals(String.format("The entry %s is expected to have the content '%s'.", entryName, expectedEntryContent), expectedEntryContent, entries.get(entryName));
    } finally {
      closer.close();
    }
  }
}
