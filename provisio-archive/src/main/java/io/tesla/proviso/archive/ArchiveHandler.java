package io.tesla.proviso.archive;

import io.provis.model.ProvisioContext;

import java.io.File;
import java.io.IOException;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;

// Encapsulate the way an output stream and entries are created for a specific archive type
public interface ArchiveHandler {
  ArchiveOutputStream getOutputStream() throws IOException;
  ArchiveInputStream getInputStream() throws IOException;
  ArchiveEntry createEntryFor(String pathInArchive, File file, ProvisioContext context);
}