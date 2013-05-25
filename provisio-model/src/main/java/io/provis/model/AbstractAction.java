package io.provis.model;

public abstract class AbstractAction implements Action {

  private String name;

  public AbstractAction() {    
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
