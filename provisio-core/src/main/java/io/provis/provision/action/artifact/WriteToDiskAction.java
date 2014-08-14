package io.provis.provision.action.artifact;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.util.FileCopier;

import java.io.File;

import javax.inject.Named;

import org.eclipse.aether.artifact.Artifact;

import com.google.common.base.Preconditions;

@Named("write")
public class WriteToDiskAction implements ProvisioningAction {
  
  private Artifact artifact;
  private File outputDirectory;
  
  public WriteToDiskAction(Artifact artifact, File outputDirectory) {
    
    Preconditions.checkArgument(outputDirectory != null, "outputDirectory cannot be null.");
    
    this.artifact = artifact;
    this.outputDirectory = outputDirectory;
  }
  
  public void execute(ProvisioningContext context) {
    File file = artifact.getFile();
    if (file != null) {
      FileCopier.copy(file, new File(outputDirectory, file.getName()));
    } 
  }
}