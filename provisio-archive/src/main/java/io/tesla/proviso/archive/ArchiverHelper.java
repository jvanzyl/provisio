package io.tesla.proviso.archive;

import io.tesla.proviso.archive.tar.TarGzArchiveHandler;
import io.tesla.proviso.archive.zip.ZipArchiveHandler;

import java.io.File;
import java.io.IOException;

public class ArchiverHelper {

  public static ArchiveHandler getArchiveHandler(File archive) throws IOException {
    ArchiveHandler archiveHandler;
    if (archive.getName().endsWith(".zip") || archive.getName().endsWith(".jar")) {
      archiveHandler = new ZipArchiveHandler(archive);
    } else if (archive.getName().endsWith(".tgz") || archive.getName().endsWith("tar.gz")) {
      archiveHandler = new TarGzArchiveHandler(archive);
    } else {
      throw new IOException("Cannot detect how to read " + archive.getName());
    }
    return archiveHandler;
  }

}
