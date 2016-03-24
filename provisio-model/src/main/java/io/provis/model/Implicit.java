package io.provis.model;

public class Implicit {

  private String name;
  private Class<?> type;
  
  public Implicit(String name, Class<?> type) {
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }
}
