package io.provis.provision.action.artifact;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;
import io.tesla.proviso.archive.UnArchiver;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.eclipse.aether.artifact.Artifact;

/**
 * The unpack is an operation that results in any number of artifacts and resources being contributed to the runtime. The archive to be unpacked can
 * make the metadata about its contents available, or we need to determine the information about the contents by examining the contents.
 * 
 * @author jvanzyl
 *
 */
@Named("unpack")
public class UnpackAction implements Action {

  //
  // Configuration
  //
  private String includes;
  private String excludes;
  private boolean useRoot;
  private boolean flatten;
  //
  private Artifact artifact;
  private File outputDirectory;
        
  public void execute(ProvisioContext context) {
    
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    File archive = artifact.getFile();
        
    try {
      
      UnArchiver unarchiver = UnArchiver.builder()
        .includes(includes)
        .excludes(excludes)
        .useRoot(useRoot)
        .flatten(flatten)
        .build();

      unarchiver.unarchive(archive, outputDirectory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String getIncludes() {
    return includes;
  }

  public void setIncludes(String includes) {
    this.includes = includes;
  }

  public String getExcludes() {
    return excludes;
  }

  public void setExcludes(String excludes) {
    this.excludes = excludes;
  }

  public boolean isUseRoot() {
    return useRoot;
  }

  public void setUseRoot(boolean useRoot) {
    this.useRoot = useRoot;
  }

  public boolean isFlatten() {
    return flatten;
  }

  public void setFlatten(boolean flatten) {
    this.flatten = flatten;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }
}