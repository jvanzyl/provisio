package io.tesla.proviso.archive;

import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

public interface ArchiveHandler {
  ArchiveOutputStream getOutputStream() throws IOException;
  ArchiveInputStream getInputStream() throws IOException;
  ExtendedArchiveEntry createEntryFor(String entryName, Entry entry, boolean isExecutable);  
}