package io.provis.provision.action.runtime;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.DefaultArchiver;

import java.io.File;

import javax.inject.Named;

@Named("archive")
public class ArchiveAction implements Action {

  private File runtimeDirectory;
  private String name;
  
  public void execute(ProvisioContext context) {
    Archiver archiver = new DefaultArchiver();
    try {
      archiver.archive(new File(runtimeDirectory, "../" + name), runtimeDirectory, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  
}