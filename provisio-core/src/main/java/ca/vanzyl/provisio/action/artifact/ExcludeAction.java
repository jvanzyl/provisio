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

import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Named("exclude")
public class ExcludeAction implements ProvisioningAction {

  private static final Logger logger = LoggerFactory.getLogger(ExcludeAction.class);

  private String dir;
  private String file;
  private File outputDirectory;

  @Override
  public void execute(ProvisioningContext context) {
    try {
      if(dir != null) {
        logger.info("Excluding directory {} in {}", dir, outputDirectory);
        Path dirToDelete = outputDirectory.toPath().resolve(dir);
        if(!Files.exists(dirToDelete)) {
          throw new RuntimeException("The excluded dir you specified doesn't exist:" + dirToDelete);
        }
        deletePath(outputDirectory.toPath().resolve(dir));
      }

      if(file != null) {
        logger.info("Excluding file {} in {}", file, outputDirectory);
        Path fileToDelete = outputDirectory.toPath().resolve(file);
        if(!Files.exists(fileToDelete)) {
          throw new RuntimeException("The excluded file you specified doesn't exist:" + fileToDelete);
        }
        Files.delete(outputDirectory.toPath().resolve(file));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public void deletePath(Path pathToBeDeleted) throws IOException {
    Files.walk(pathToBeDeleted)
       .sorted(Comparator.reverseOrder())
       .map(Path::toFile)
       .forEach(File::delete);
  }
}
