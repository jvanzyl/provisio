package io.provis.model;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;

public class ProvisioningResult {
  
  private List<File> archives;
  
  public List<File> getArchives() {
    return archives;
  }

  public void addArchive(File archive) {
    if(archives == null) {
      archives = Lists.newArrayList();
    }
    archives.add(archive);
  }
}
