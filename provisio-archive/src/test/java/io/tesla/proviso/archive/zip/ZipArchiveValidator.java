package io.tesla.proviso.archive.zip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import io.tesla.proviso.archive.ArchiverValidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;

public class ZipArchiveValidator implements ArchiverValidator {

  public void assertNumberOfEntriesInArchive(int entries, File archive) throws IOException {
    ZipFile zipFile = new ZipFile(archive);
    assertEquals(String.format("Expected %s entries.", zipFile.size()), entries, zipFile.size());
  }

  public void assertPresenceOfEntryInArchive(File archive, String entryName) throws IOException {
    boolean returnValue;
    ZipFile zipFile = new ZipFile(archive);
    ZipEntry entry = zipFile.getEntry(entryName);
    if (entry != null) {
      returnValue = true;
    } else {
      returnValue = false;
    }
    assertTrue(String.format("The entry %s is expected to be present, but is not.", entryName), returnValue);
  }

  public void assertAbsenceOfEntryInArchive(File archive, String entryName) throws IOException {
    boolean returnValue;
    ZipFile zipFile = new ZipFile(archive);
    ZipEntry entry = zipFile.getEntry(entryName);
    if (entry != null) {
      returnValue = true;
    } else {
      returnValue = false;
    }
    assertFalse(String.format("The entry %s is not expected to be present, but it is.", entryName), returnValue);
  }

  public void assertContentOfEntryInArchive(File archive, String entryName, String expectedEntryContent) throws IOException {
    boolean returnValue;
    ZipFile zipFile = new ZipFile(archive);
    ZipEntry entry = zipFile.getEntry(entryName);
    if (entry != null) {
      returnValue = true;
    } else {
      returnValue = false;
    }    
    Closer closer = Closer.create();
    try {
      Reader reader = closer.register(new InputStreamReader(zipFile.getInputStream(entry), Charsets.UTF_8));
      String entryContent = CharStreams.toString(reader);
      assertTrue(String.format("The entry %s is expected to be present, but is not.", entryName), returnValue);
      assertEquals(String.format("The entry %s is expected to have the content '%s'.", entryName, expectedEntryContent), expectedEntryContent, entryContent);
    } finally {
      closer.close();
    }
  }
}
