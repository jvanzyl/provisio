/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.airlift;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

import io.provis.SimpleProvisioner;
import io.tesla.proviso.archive.UnArchiver;

@Named(AirliftProvisioner.ID)
public class AirliftProvisioner extends SimpleProvisioner {

  public static final String ID = "airlift";
  
  public AirliftProvisioner() {
    super();
  }
  
  public AirliftProvisioner(File localRepository, String remoteRepository) {
    super(localRepository, remoteRepository);
  }

  public File provision(AirliftProvisioningContext context) throws IOException {
    File serverHome = context.getServerHome();
    String repositoryUrl = context.getRepositoryUrl();    
    File airliftServerTarGz = resolveFromRepository(repositoryUrl, context.getServerCoordinate());    
    UnArchiver unarchiver = UnArchiver.builder().useRoot(false).flatten(false).build();
    FileUtils.mkdir(serverHome.getAbsolutePath());
    unarchiver.unarchive(airliftServerTarGz, serverHome);
    createWin32Launcher(new File(serverHome, "bin"));
    return serverHome;
  }
  
  private void createWin32Launcher(File dir) throws IOException {
    File outFile = new File(dir, "launcher_win.py");
    if(outFile.exists()) {
      return;
    }
    try(InputStream in = this.getClass().getResourceAsStream("/launcher_win.py"); OutputStream out = new FileOutputStream(outFile) ) {
      byte[] buf = new byte[4096];
      int l;
      while((l = in.read(buf)) != -1) {
        out.write(buf, 0, l);
      }
    }
  }
  
}
