package io.provis.provision.action.runtime;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.Archiver.ArchiverBuilder;

import java.io.File;

import javax.inject.Named;

import org.codehaus.plexus.util.StringUtils;

public class ArchiveAction implements ProvisioningAction {

  private String name;
  private String executable;
  private File runtimeDirectory;

  public void execute(ProvisioningContext context) {
    ArchiverBuilder builder = Archiver.builder();

    if (executable != null) {
      builder.executable(StringUtils.split(executable, ","));
    }

    Archiver archiver = builder.build();

    try {
      File archive = new File(runtimeDirectory, "../" + name).getCanonicalFile();
      archiver.archive(archive, runtimeDirectory);
      //
      // Right now this action has some special meaning it maybe shouldn't, but we need to know what archives are produced
      // so that we can set/attach the artifacts in a MavenProject.
      //
      context.getResult().addArchive(archive);
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