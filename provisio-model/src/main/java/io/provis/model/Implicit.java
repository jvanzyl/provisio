package io.provis.model;

public class Implicit {

  private String name;
  private Class<?> type;
  private Class<?> itemType;
  
  public Implicit(String name, Class<?> type) {
    this(name, type, null);
  }
  
  public Implicit(String name, Class<?> type, Class<?> itemType) {
    this.name = name;
    this.type = type;
    this.itemType = itemType;
  }

  public String getName() {
    return name;
  }

  public Class<?> getType() {
    return type;
  }
  
  public Class<?> getItemType() {
    return itemType;
  }
}
