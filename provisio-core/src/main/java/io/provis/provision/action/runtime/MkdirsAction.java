/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.provision.action.runtime;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;

import java.io.File;

import javax.inject.Named;

@Named("mkdirs")
class MkdirsAction implements ProvisioningAction {

  private File directory;
    
  public void execute(ProvisioningContext context) {
    new File(directory, "var/log").mkdirs();
    new File(directory, "var/tmp").mkdirs();
  }
}