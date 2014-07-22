package io.tesla.proviso.archive.source;

import io.tesla.proviso.archive.Entry;
import io.tesla.proviso.archive.Source;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class FileSource implements Source {

  private final String archiveEntryName;
  private final File file;
  private boolean hasNext = true;

  public FileSource(File file) {
    this.archiveEntryName = file.getName();
    this.file = file;
  }

  public FileSource(String archiveEntryName, File file) {
    this.archiveEntryName = archiveEntryName;
    this.file = file;
  }

  @Override
  public Iterable<Entry> entries() {
    return new Iterable<Entry>() {
      @Override
      public Iterator<Entry> iterator() {
        return new Iterator<Entry>() {
          @Override
          public boolean hasNext() {
            return hasNext;
          }

          @Override
          public Entry next() {
            hasNext = false;
            return new FileEntry(archiveEntryName, file);
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove method not implemented");
          }
        };
      }
    };
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public boolean isDirectory() {
    return false;
  }
}
