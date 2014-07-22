package io.tesla.proviso.archive;

import java.io.File;
import java.io.IOException;

public interface ArchiverValidator {
  public void assertNumberOfEntriesInArchive(int expectedEntries, File archive) throws IOException;
  public void assertPresenceOfEntryInArchive(File archive, String entryName) throws IOException;
  public void assertAbsenceOfEntryInArchive(File archive, String entryName) throws IOException;
  public void assertContentOfEntryInArchive(File archive, String entryName, String expectedEntryContent) throws IOException;
}
