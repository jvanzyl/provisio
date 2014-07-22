package io.tesla.proviso.archive;

import java.io.IOException;

public interface Source {
  Iterable<Entry> entries();
  boolean isDirectory();
  void close() throws IOException;
}
