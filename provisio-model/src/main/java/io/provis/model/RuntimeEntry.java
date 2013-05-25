package io.provis.model;

public class RuntimeEntry {
  
  private String name;
  private int mode;
  
  public RuntimeEntry(String name, int mode) {
    this.name = name;
    this.mode = mode;
  }
  
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public int getMode() {
    return mode;
  }
  public void setMode(int mode) {
    this.mode = mode;
  }
}
