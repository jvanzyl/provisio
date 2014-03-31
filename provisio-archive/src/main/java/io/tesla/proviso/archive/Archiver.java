package io.tesla.proviso.archive;

import io.provis.model.ProvisioContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Closer;

// There should be a full inventory of what has gone into the archive, so that we can capture the contents.
//  
//    Archiver archiver = Archiver.builder()
//        .includes("**/*.java")
//        .includes(Iterable<String>)
//        .excludes("**/*.properties")
//        .excludes(Iterable<String>)
//        .flatten(true)
//        .useRoot(false)
//        .build();
//
public class Archiver {

  private final List<String> includes;
  private final List<String> excludes;
  private final boolean useRoot;

  public Archiver(List<String> includes, List<String> excludes, boolean useRoot) {
    this.includes = includes;
    this.excludes = excludes;
    this.useRoot = useRoot;
  }

  public void archive(File archive, File sourceDirectory) throws IOException {
    archive(archive, sourceDirectory, null);
  }

  public void archive(File archive, File sourceDirectory, ProvisioContext context) throws IOException {

    String type = "";
    String archiveBaseDirectory = sourceDirectory.getName();
    Closer outputCloser = Closer.create();
    try {
      ArchiveOutputStream aos = null;


    if (archive.getName().endsWith(".zip") || archive.getName().endsWith(".jar")) {
      aos = new ZipArchiveOutputStream(new FileOutputStream(archive));
      type = "zip";
    } else if (archive.getName().endsWith(".tgz") || archive.getName().endsWith("tar.gz")) {
      aos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(archive)));
      ((TarArchiveOutputStream) aos).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      type = "targz";
    }

    if (aos == null) {
      throw new IOException("Cannot detect how to read " + archive.getName());
    }

    //TarArchiveOutputStream aos = (TarArchiveOutputStream) new ArchiveStreamFactory().createArchiveOutputStream("tar", out);

    DirectoryScanner ds = new DirectoryScanner();
    if (!includes.isEmpty()) {
      ds.setIncludes(includes.toArray(new String[includes.size()]));
    }
    if (!excludes.isEmpty()) {
      ds.setExcludes(excludes.toArray(new String[excludes.size()]));
    }
    ds.setBasedir(sourceDirectory);
    ds.setCaseSensitive(true);
    ds.scan();

    if (type.equals("targz")) {
      for (String filePath : ds.getIncludedFiles()) {
        File file = new File(sourceDirectory, filePath);
        String entryName = archiveBaseDirectory + "/" + filePath;
        if (useRoot == false) {
          entryName = entryName.substring(entryName.indexOf('/') + 1);
        }
        TarArchiveEntry entry = new TarArchiveEntry(entryName);
        entry.setSize(file.length());
        if (context != null && context.getFileEntries().get(entryName) != null) {
          entry.setMode(context.getFileEntries().get(entryName).getMode());
        }
        aos.putArchiveEntry(entry);
        Closer inputCloser = Closer.create();
        try {
          InputStream entryInputStream = inputCloser.register(new FileInputStream(file));
          IOUtils.copy(entryInputStream, aos);
        } finally {
          inputCloser.close();
        }
        aos.closeArchiveEntry();
      }
    } else if (type.equals("zip")) {
      for (String filePath : ds.getIncludedFiles()) {
        File file = new File(sourceDirectory, filePath);
        String entryName = archiveBaseDirectory + "/" + filePath;
        if (useRoot == false) {
          entryName = entryName.substring(entryName.indexOf('/') + 1);
        }
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        entry.setSize(file.length());
        if (context != null && context.getFileEntries().get(entryName) != null) {
          entry.setUnixMode(context.getFileEntries().get(entryName).getMode());
        }
        aos.putArchiveEntry(entry);
        Closer inputCloser = Closer.create();
        try {
          InputStream entryInputStream = inputCloser.register(new FileInputStream(file));
          IOUtils.copy(entryInputStream, aos);
        } finally {
          inputCloser.close();
        }
        aos.closeArchiveEntry();
      }
    }
    aos.finish();
    } finally {
      outputCloser.close();
    }
  }

  public static ArchiverBuilder builder() {
    return new ArchiverBuilder();
  }

  public static class ArchiverBuilder {

    private List<String> includes = new ArrayList<String>();
    private List<String> excludes = new ArrayList<String>();
    private boolean useRoot = true;

    public ArchiverBuilder includes(String... includes) {
      return includes(ImmutableList.copyOf(includes));
    }

    public ArchiverBuilder includes(Iterable<String> includes) {
      Iterables.addAll(this.includes, includes);
      return this;
    }

    public ArchiverBuilder excludes(String... excludes) {
      return excludes(ImmutableList.copyOf(excludes));
    }

    public ArchiverBuilder excludes(Iterable<String> excludes) {
      Iterables.addAll(this.excludes, excludes);
      return this;
    }

    public ArchiverBuilder useRoot(boolean useRoot) {
      this.useRoot = useRoot;
      return this;
    }

    public Archiver build() {
      return new Archiver(includes, excludes, useRoot);
    }

  }

}
