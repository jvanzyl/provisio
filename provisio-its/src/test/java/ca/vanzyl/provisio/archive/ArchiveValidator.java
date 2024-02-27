package ca.vanzyl.provisio.archive;

import java.io.IOException;

public interface ArchiveValidator {

    void assertNumberOfEntriesInArchive(int expectedEntries) throws IOException;

    void assertContentOfEntryInArchive(String entryName, String expectedEntryContent) throws IOException;

    void assertSizeOfEntryInArchive(String entryName, long size) throws IOException;

    void assertTimeOfEntryInArchive(String entryName, long time) throws IOException;

    void assertEntryExists(String expectedEntry) throws IOException;

    void assertEntryDoesntExist(String expectedEntry) throws IOException;

    void assertEntries(String... entries) throws IOException;

    void assertSortedEntries(String... entries) throws IOException;

    void showEntries();
}
