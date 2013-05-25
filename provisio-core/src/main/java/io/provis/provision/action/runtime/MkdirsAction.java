package io.provis.provision.action.runtime;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import java.io.File;

import javax.inject.Named;

@Named("mkdirs")
class MkdirsAction implements Action {

  private File directory;
    
  public void execute(ProvisioContext context) {
    new File(directory, "var/log").mkdirs();
    new File(directory, "var/tmp").mkdirs();
  }
}