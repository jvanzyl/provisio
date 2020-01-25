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
package ca.vanzyl.provisio.action.fileset;

import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

@Named("executable")
public class MakeExecutableAction implements ProvisioningAction {

  private String includes;
  private String excludes;
  private File fileSetDirectory;
  private File runtimeDirectory;

  public void execute(ProvisioningContext context) throws Exception {

    if (fileSetDirectory.exists()) {
      try {
        List<String> filePaths = FileUtils.getFileNames(fileSetDirectory, includes, excludes, true);
        for (String filePath : filePaths) {
          File file = new File(filePath);
          file.setExecutable(true);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
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

  public File getFileSetDirectory() {
    return fileSetDirectory;
  }

  public void setFileSetDirectory(File fileSetDirectory) {
    this.fileSetDirectory = fileSetDirectory;
  }

  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }
}
