package io.provis.provision.action.artifact;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;
import io.provis.util.FileCopier;

import java.io.File;

import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.artifact.Artifact;

import com.google.common.base.Preconditions;

@Singleton
@Named("write")
public class WriteToDiskAction implements Action {
  
  private Artifact artifact;
  private File outputDirectory;
  
  public WriteToDiskAction(Artifact artifact, File outputDirectory) {
    
    Preconditions.checkArgument(outputDirectory != null, "outputDirectory cannot be null.");
    
    this.artifact = artifact;
    this.outputDirectory = outputDirectory;
  }
  
  public void execute(ProvisioContext context) {
    File file = artifact.getFile();
    if (file != null) {
      FileCopier.copy(file, new File(outputDirectory, file.getName()));
    } 
  }
}