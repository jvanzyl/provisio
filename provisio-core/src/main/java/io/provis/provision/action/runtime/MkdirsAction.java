package io.provis.provision.action.runtime;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

import java.io.File;

import javax.inject.Named;

@Named("mkdirs")
class MkdirsAction implements ProvisioningAction {

  private File directory;
    
  public void execute(ProvisioningContext context) {
    new File(directory, "var/log").mkdirs();
    new File(directory, "var/tmp").mkdirs();
  }
}