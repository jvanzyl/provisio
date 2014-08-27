package io.provis.model;

import java.util.List;

public class Directory {

  private String path;
  private List<Include> includes;
  private List<Exclude> excludes;
  
  public String getPath() {
    return path;
  }
  
  public List<Include> getIncludes() {
    return includes;
  }

  public List<Exclude> getExcludes() {
    return excludes;
  }



  public class Include {
    String name;

    public String getName() {
      return name;
    }    
  }
  
  public class Exclude {
    String name;

    public String getName() {
      return name;
    }    
  }
}
