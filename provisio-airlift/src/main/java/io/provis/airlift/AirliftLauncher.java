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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.airlift.command.Command;

public class AirliftLauncher {

  private final File serverHome;
  private final String statusUrl;
  private static final Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("airlift-%s").build());

  public AirliftLauncher(AirliftProvisioningContext context) throws Exception {
    this.serverHome = context.getServerHome();
    this.statusUrl = context.getStatusUrl();
  }

  public void start() throws Exception {
    
    Command cmd;
    if(File.pathSeparatorChar == ';') {
      createWin32LauncherPy(new File(serverHome, "bin"));
      cmd = new Command("cmd", "/c", "start", "bin/launcher_win.py", "run");
    } else {
      cmd = new Command("bin/launcher", "start");
    }
    
    cmd.setDirectory(serverHome)
      .setTimeLimit(5, TimeUnit.MINUTES)
      .execute(executor);
    System.out.println("Attempting to determine if Airlift server is ready!");
    while (!readyToRespondToRequests()) {
      Thread.sleep(3000);
    }
  }

  public void stop() throws Exception {
    Command cmd;
    if(File.pathSeparatorChar == ';') {
      // the new launcher should have already been created
      cmd = new Command("python.exe", "bin/launcher_win.py", "stop");
    } else {
      cmd = new Command("bin/launcher", "stop");
    }
    
    cmd.setDirectory(serverHome)
      .setTimeLimit(5, TimeUnit.MINUTES)
      .execute(executor);
  }

  private boolean readyToRespondToRequests() {
    HttpURLConnection connection = null;
    try {
      URL serverAddress = new URL(statusUrl);
      connection = (HttpURLConnection) serverAddress.openConnection();
      connection.setRequestMethod("GET");
      connection.setReadTimeout(2000);
      connection.connect();
      if (connection.getResponseCode() != 200) {
        return false;
      }
    } catch (Exception e) {
      return false;
    } finally {
      connection.disconnect();
      connection = null;
    }
    return true;
  }
  
  private void createWin32LauncherPy(File dir) throws IOException {
    
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
