package io.provis.action.artifact.alter;

import java.util.List;

import com.google.common.collect.Lists;

import io.provis.model.File;

public class Delete {
  
  private List<File> files = Lists.newArrayList();  
  
  public List<File> getFiles() {
    return files;
  }  
}
