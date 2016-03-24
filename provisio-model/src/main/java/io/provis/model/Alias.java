package io.provis.model;

public class Alias {

  private String name;
  private Class<?> type;
  
  public Alias(String name, Class<?> type) {
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
