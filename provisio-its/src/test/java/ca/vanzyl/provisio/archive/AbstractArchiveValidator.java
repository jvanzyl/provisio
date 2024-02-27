package ca.vanzyl.provisio.archive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public abstract class AbstractArchiveValidator implements ArchiveValidator {

    protected final MultiMap<String, TestEntry> entries;

    protected AbstractArchiveValidator(Source source) throws IOException {
        MultiMap<String, TestEntry> entries = new MultiMap<>();
        for (ExtendedArchiveEntry entry : source.entries()) {
            OutputStream outputStream = new ByteArrayOutputStream();
            entry.getInputStream().transferTo(outputStream);
            entries.put(
                    entry.getName(),
                    new TestEntry(entry.getName(), outputStream.toString(), entry.getTime(), entry.getSize()));
        }
        this.entries = entries;
    }

    @Override
    public void assertEntries(String... expectedEntries) throws IOException {
        String expected = toString(Arrays.asList(expectedEntries));
        List<String> actual = new ArrayList<>();
        for (Entry<String, List<TestEntry>> entry : entries.entries()) {
            actual.add(entry.getKey());
        }
        assertEquals("Archive entries", expected, toString(actual));
    }

    @Override
    public void assertEntryExists(String expectedEntry) {
        assertTrue("Expected to find " + expectedEntry, entries.containsKey(expectedEntry));
    }

    @Override
    public void assertEntryDoesntExist(String missingEntry) {
        assertFalse("Expected not to find " + missingEntry, entries.containsKey(missingEntry));
    }

    private String toString(Collection<String> strings) {
        List<String> sorted = new ArrayList<>(strings);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder();
        for (String string : sorted) {
            sb.append(string).append('\n');
        }
        return sb.toString();
    }

    @Override
    public void assertSortedEntries(String... expectedEntries) {
        String expected = toString(Arrays.asList(expectedEntries));
        List<String> actual = new ArrayList<>();
        for (Entry<String, List<TestEntry>> entry : entries.entries()) {
            actual.add(entry.getKey());
        }
        assertEquals("Archive entries", expected, toNonSortedString(actual));
    }

    private String toNonSortedString(Collection<String> strings) {
        List<String> sorted = new ArrayList<>(strings);
        StringBuilder sb = new StringBuilder();
        for (String string : sorted) {
            sb.append(string).append('\n');
        }
        return sb.toString();
    }

    @Override
    public void assertNumberOfEntriesInArchive(int expectedEntries) {
        assertEquals("Number of archive entries", expectedEntries, entries.size());
    }

    @Override
    public void assertContentOfEntryInArchive(String entryName, String expectedEntryContent) {
        List<String> values =
                entries.get(entryName).stream().map(e -> e.content).collect(Collectors.toList());
        assertEquals(String.format("Number of archive entries with path  %s", entryName), 1, values.size());
        assertEquals(String.format("Archive entry %s contents", entryName), expectedEntryContent, values.get(0));
    }

    @Override
    public void assertSizeOfEntryInArchive(String entryName, long size) {
        List<Long> values = entries.get(entryName).stream().map(e -> e.size).collect(Collectors.toList());
        assertEquals(String.format("Number of archive entries with path %s", entryName), 1, values.size());
        assertEquals(String.format("Archive entry %s size", entryName), size, (long) values.get(0));
    }

    @Override
    public void assertTimeOfEntryInArchive(String entryName, long time) {
        List<Long> values = entries.get(entryName).stream().map(e -> e.time).collect(Collectors.toList());
        assertEquals(String.format("Number of archive entries with path  %s", entryName), 1, values.size());
        assertEquals(
                String.format("Archive entry %s time", entryName),
                time,
                values.get(0).longValue());
    }

    @Override
    public void showEntries() {
        System.out.println();
        entries.values().forEach(entry -> System.out.println(entry.name + " -> " + entry.size));
        System.out.println();
    }

    static class TestEntry {

        String name;
        String content;
        long time;
        long size;

        TestEntry(String name, String content, long time, long size) {
            this.name = name;
            this.content = content;
            this.time = time;
            this.size = size;
        }
    }
}
