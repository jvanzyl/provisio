package io.provis.action;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioContext;

import java.util.List;

import javax.inject.Named;

@Named("makeExecutable")
public class MakeExecutable implements ProvisioningAction {

  private List<String> includes;
  private List<String> excludes;
  
  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }
}
