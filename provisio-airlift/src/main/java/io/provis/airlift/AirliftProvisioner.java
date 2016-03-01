/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.airlift;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

import io.provis.provision.SimpleProvisioner;
import io.tesla.proviso.archive.UnArchiver;

@Named(AirliftProvisioner.ID)
public class AirliftProvisioner extends SimpleProvisioner {

  public static final String ID = "airlift";

  public File provision(AirliftProvisioningContext context) throws IOException {
    File serverHome = context.getServerHome();
    String repositoryUrl = context.getRepositoryUrl();    
    File airliftServerTarGz = resolveFromRepository(repositoryUrl, context.getServerCoordinate());    
    UnArchiver unarchiver = UnArchiver.builder().useRoot(false).flatten(false).build();
    FileUtils.mkdir(serverHome.getAbsolutePath());
    unarchiver.unarchive(airliftServerTarGz, serverHome);
    return serverHome;
  }
}
