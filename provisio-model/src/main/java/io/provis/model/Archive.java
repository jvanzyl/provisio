package io.provis.model;

import java.io.File;

public class Archive {
  
  private File file;
  private String classifier;

  public Archive(File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  @Override
  public String toString() {
    return "Archive [file=" + file + ", classifier=" + classifier + "]";
  }
}
