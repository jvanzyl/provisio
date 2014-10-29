package io.provis.provision.action.artifact;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.provision.ProvisioningException;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.eclipse.aether.artifact.Artifact;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

@Named("write")
public class WriteToDiskAction implements ProvisioningAction {
  
  private Artifact artifact;
  private File outputDirectory;
  
  public WriteToDiskAction(Artifact artifact, File outputDirectory) {    
    Preconditions.checkArgument(outputDirectory != null, "outputDirectory cannot be null.");    
    this.artifact = artifact;
    this.outputDirectory = outputDirectory;
  }
  
  @Override
  public void execute(ProvisioningContext context) {
    File file = artifact.getFile();
    if (file != null) {
      copy(file, new File(outputDirectory, file.getName()));
    } 
  }
  
  public void copy(File source, File target) {
    try {
      if(target.getParentFile().exists() == false) {
        target.getParentFile().mkdirs();
      }
      Files.copy(source, target);
    } catch (IOException e) {
      throw new ProvisioningException("Error copying " + source + " to " + target, e);
    }
  }  
}