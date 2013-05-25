package io.tesla.proviso.archive;

import io.provis.model.RuntimeEntry;
import io.provis.model.ProvisioContext;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveException;

public interface Archiver {

  void archive(File archive, File sourceDirectory, ProvisioContext context, String includes, String excludes, boolean useRoot, boolean flatten) throws IOException, ArchiveException;
  void archive(File archive, File sourceDirectory, ProvisioContext context) throws IOException, ArchiveException;
  
  Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory) throws IOException;  
  Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory, String includes, String excludes, boolean useRoot, boolean flatten) throws IOException;
}
