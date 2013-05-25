package io.provis.action;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import java.util.List;

import javax.inject.Named;

@Named("unpack")
public class Unpack implements Action {

  private boolean useRoot;
  private List<String> excludes;
  
  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  public boolean isUseRoot() {
    return useRoot;
  }

  public void setUseRoot(boolean useRoot) {
    this.useRoot = useRoot;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }
  
  
}
