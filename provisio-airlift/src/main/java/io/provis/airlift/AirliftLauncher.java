/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.airlift;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.airlift.command.Command;
import io.airlift.command.CommandFailedException;
import io.airlift.command.CommandResult;

public class AirliftLauncher {

  private final File serverHome;
  private final String statusUrl;
  private static final Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("airlift-%s").build());

  public AirliftLauncher(AirliftProvisioningContext context) throws Exception {
    this.serverHome = context.getServerHome();
    this.statusUrl = context.getStatusUrl();
  }

  public void start() throws Exception {
    Command cmd = createCmd("start", true);
    
    long maxTime = cmd.getTimeLimit().toMillis();
    long start = System.currentTimeMillis();
    
    cmd.execute(executor);
    
    System.out.println("Attempting to determine if Airlift server is ready!");
    boolean seenRunning = false;
    while (!readyToRespondToRequests()) {
      boolean running = isRunning();
      
      if(seenRunning && !running) {
        throw new CommandFailedException(cmd, "Process terminated unexpectedly", null);
      }
      seenRunning |= running;
      
      long total = System.currentTimeMillis() - start;
      
      // if the process doesn't start in 10s
      if(!seenRunning && total > 10000L) {
        throw new CommandFailedException(cmd, "Process did not start in timely manner", null);
      }
      
      // if it doesn't start accepting connections during maxTime
      if(total > maxTime) { 
        throw new CommandFailedException(cmd, "Process did not start in timely manner", null);
      }
      
      Thread.sleep(3000);
    }
  }

  public void stop() throws Exception {
    createCmd("stop", false).execute(executor);
  }
  
  public boolean isRunning() throws Exception {
    CommandResult res = createCmd("status", false)
        .setSuccessfulExitCodes(0, 3).execute(executor);
    return res.getExitCode() == 0;
  }

  private Command createCmd(String command, boolean forks) {
    Command cmd;
    if(File.pathSeparatorChar == ';') {
      if(forks) {
        cmd = new Command("cmd", "/c", "start", "bin/launcher_win.py", command);
      } else {
        cmd = new Command("cmd", "/c", "python.exe", "bin/launcher_win.py", command);
      }
    } else {
      cmd = new Command("bin/launcher", command);
    }
    
    return cmd.setDirectory(serverHome)
        .setTimeLimit(5, TimeUnit.MINUTES);
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
}
