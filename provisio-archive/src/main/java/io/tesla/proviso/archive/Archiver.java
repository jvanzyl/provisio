package io.tesla.proviso.archive;

import io.tesla.proviso.archive.source.DirectorySource;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.codehaus.plexus.util.SelectorUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;

public class Archiver {

  private final List<String> includes;
  private final List<String> excludes;
  private final List<String> executables;
  private final boolean useRoot;
  private final boolean flatten;
  
  private Archiver(List<String> includes, List<String> excludes, List<String> executables, boolean useRoot, boolean flatten) {
    this.includes = includes;
    this.excludes = excludes;
    this.executables = executables;
    this.useRoot = useRoot;
    this.flatten = flatten;
  }

  public void archive(File archive, List<String> sourceDirectories) throws IOException {
    File[] fileSourceDirectories = new File[sourceDirectories.size()];
    for (int i = 0; i < sourceDirectories.size(); i++) {
      fileSourceDirectories[i] = new File(sourceDirectories.get(i));
    }
    archive(archive, fileSourceDirectories);
  }

  public void archive(File archive, File... sourceDirectories) throws IOException {
    archive(archive, new DirectorySource(sourceDirectories));
  }

  public void archive(File archive, Source... sources) throws IOException {
    ArchiveHandler archiveHandler = ArchiverHelper.getArchiveHandler(archive);
    Closer closer = Closer.create();
    try {
      ArchiveOutputStream aos = closer.register(archiveHandler.getOutputStream());
      for (Source source : sources) {
        for (Entry entry : source.entries()) {
          String entryName = entry.getName();
          boolean exclude = false;
          if (!excludes.isEmpty()) {
            for (String excludePattern : excludes) {
              if (SelectorUtils.match(excludePattern, entryName)) {
                exclude = true;
                break;
              }
            }
          }
          if (exclude) {
            continue;
          }          
          boolean include = true;
          if (!includes.isEmpty()) {
            for (String includePattern : includes) {
              if (!SelectorUtils.match(includePattern, entryName)) {
                include = false;
                break;
              }
            }
          }             
          if (!include) {
            continue;
          }          
          if (!useRoot && source.isDirectory()) {
            entryName = entryName.substring(entryName.indexOf('/') + 1);
          }
          if (flatten && source.isDirectory()) {
            entryName = entryName.substring(entryName.lastIndexOf('/') + 1);
          }
          boolean isExecutable = false;
          for (String executable : executables) {
            if (SelectorUtils.match(executable, entry.getName())) {
              isExecutable = true;
              break;
            }
          }
          ExtendedArchiveEntry archiveEntry = archiveHandler.createEntryFor(entryName, entry, isExecutable);
          aos.putArchiveEntry(archiveEntry);
          entry.writeEntry(aos);
          aos.closeArchiveEntry();
        }
        source.close();
      }
    } finally {
      closer.close();
    }
  }

  public static ArchiverBuilder builder() {
    return new ArchiverBuilder();
  }

  public static class ArchiverBuilder {
    private List<String> includes = Lists.newArrayList();
    private List<String> excludes = Lists.newArrayList();
    private List<String> executables = Lists.newArrayList();
    private boolean useRoot = true;
    private boolean flatten = false;

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
      return new Archiver(includes, excludes, executables, useRoot, flatten);
    }

    public ArchiverBuilder executable(String... executables) {
      return executable(ImmutableList.copyOf(executables));
    }

    public ArchiverBuilder executable(Iterable<String> executables) {
      Iterables.addAll(this.executables, executables);
      return this;
    }

    public ArchiverBuilder flatten(boolean flatten) {
      this.flatten = flatten;
      return this;
    }
  }
}
