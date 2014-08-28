package io.provis.model;

import java.util.List;

import com.google.common.collect.Lists;

public class FileSet {

  private String to;
  private List<File> files;
  private List<Directory> directories;

  public String getDirectory() {
    return to;
  }

  public List<File> getFiles() {
    if (files == null) {
      files = Lists.newArrayList();
    }
    return files;
  }

  public List<Directory> getDirectories() {
    if (directories == null) {
      directories = Lists.newArrayList();
    }
    return directories;
  }

  @Override
  public String toString() {
    return "FileSet [directory=" + to + ", files=" + files + ", directories=" + directories + "]";
  }

}
