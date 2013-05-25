package io.provis.action;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import java.util.Map;

import javax.inject.Named;

@Named("updateJar")
public class UpdateJar implements Action {

  private String jar;
  private Map<String,String> updates;
  
  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  public String getJar() {
    return jar;
  }

  public void setJar(String jar) {
    this.jar = jar;
  }

  public Map<String, String> getUpdates() {
    return updates;
  }

  public void setUpdates(Map<String, String> updates) {
    this.updates = updates;
  }
  
  
}
