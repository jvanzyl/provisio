/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.action.artifact;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import io.provis.ProvisioningException;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

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
      if (target.getParentFile().exists() == false) {
        target.getParentFile().mkdirs();
      }
      Files.copy(source, target);
    } catch (IOException e) {
      throw new ProvisioningException("Error copying " + source + " to " + target, e);
    }
  }
}
