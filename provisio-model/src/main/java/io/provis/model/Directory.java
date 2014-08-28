package io.provis.model;

import java.util.List;

import com.google.common.collect.Lists;

public class Directory {

  private String path;
  private List<String> includes;
  private List<String> excludes;

  public String getPath() {
    return path;
  }

  public List<String> getIncludes() {
    if (includes == null) {
      includes = Lists.newArrayList();
    }
    return includes;
  }

  public List<String> getExcludes() {
    if (excludes == null) {
      excludes = Lists.newArrayList();
    }
    return excludes;
  }
}
