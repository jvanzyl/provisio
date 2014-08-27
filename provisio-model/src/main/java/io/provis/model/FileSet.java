package io.provis.model;

import java.util.List;

public class FileSet {

  private String to;
  private List<File> files;
  private List<Directory> directories;
    
  public String getDirectory() {
    return to;
  }
  
  public List<File> getFiles() {
    return files;
  }

  public List<Directory> getDirectories() {
    return directories;
  }

  @Override
  public String toString() {
    return "FileSet [directory=" + to + ", files=" + files + ", directories=" + directories + "]";
  }
  
  
}
