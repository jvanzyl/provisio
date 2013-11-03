package io.provis.model;

import java.util.HashMap;
import java.util.Map;

public class ProvisioContext {
  
  private Map<String,RuntimeEntry> fileEntries = new HashMap<String,RuntimeEntry>();

  public Map<String,RuntimeEntry> getFileEntries() {
    return fileEntries;
  }

  public void setFileEntries(Map<String, RuntimeEntry> fileEntries) {
    this.fileEntries = fileEntries;
  }
  
}
