package io.provis.model;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

public class Unpack implements ProvisioningAction {

  private String includes;
  private String excludes;
  private boolean useRoot;
  private boolean flatten;
  private boolean filter;

  @Override
  public String toString() {
    return "Unpack [includes=" + includes + ", excludes=" + excludes + ", useRoot=" + useRoot + ", flatten=" + flatten + ", filter=" + filter + "]";
  }

  @Override
  public void execute(ProvisioningContext context) throws Exception {
    System.out.println("Hello, I was instantiated by XStream!!");
    System.out.println(this);
  }
}
