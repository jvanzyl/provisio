/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.action.runtime;

import java.io.File;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

public class MakeDirectoryAction implements ProvisioningAction {

  private File runtimeDirectory;
  private String name;

  public void execute(ProvisioningContext context) {
    File directoryToMake = new File(runtimeDirectory, name);
    if(!directoryToMake.mkdirs()) {
      throw new RuntimeException(String.format("Unable to create the directory %s", directoryToMake));
    }
  }
  
  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }  
}
