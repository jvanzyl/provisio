/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.action.artifact;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

import ca.vanzyl.provisio.ProvisioningException;

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
