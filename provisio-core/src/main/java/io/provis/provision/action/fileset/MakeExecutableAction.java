/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.provision.action.fileset;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

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
