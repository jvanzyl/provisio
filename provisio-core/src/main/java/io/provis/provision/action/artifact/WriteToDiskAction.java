package io.provis.provision.action.artifact;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.provision.ProvisioningException;

@Named("write")
public class WriteToDiskAction implements ProvisioningAction {
  
  private ProvisioArtifact artifact;
  private File outputDirectory;
  
  public WriteToDiskAction(ProvisioArtifact artifact, File outputDirectory) {    
    Preconditions.checkArgument(outputDirectory != null, "outputDirectory cannot be null.");    
    this.artifact = artifact;
    this.outputDirectory = outputDirectory;
  }
  
  @Override
  public void execute(ProvisioningContext context) {
    File file = artifact.getFile();
    if (file != null) {
      String targetName = artifact.getName() != null ? artifact.getName() : file.getName();
      copy(file, new File(outputDirectory, targetName));
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