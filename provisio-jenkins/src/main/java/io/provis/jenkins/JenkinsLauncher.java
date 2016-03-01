/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.jenkins;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.Commandline;

import io.provis.nexus.Command;

public class JenkinsLauncher {

  private File installationDirectory;
  private File jenkinsWar;
  private File workDirectory;
  private int port;
  private Commandline cl;
  private Command command;

  public JenkinsLauncher(JenkinsProvisioningContext context) throws Exception {
    this.installationDirectory = context.getInstallationDirectory();
    this.jenkinsWar = new File(this.installationDirectory, String.format("jenkins-war-%s.war", context.getVersion()));
    this.workDirectory = context.getWorkDirectory();
    this.port = context.getPort();
  }

  public void start() throws Exception {
    FileUtils.mkdir(workDirectory.getAbsolutePath());
    // java $vmArgs -cp ${classPath} ${mainClass} ${programArguments}
    cl = new Commandline();
    cl.setWorkingDirectory(installationDirectory);
    cl.addArguments(new String[] {
        "java", "-Xms256m", "-Xmx1024m",
    });
    cl.addArguments(getVMArguments());
    cl.addArguments(new String[] {
        "-jar", jenkinsWar.getAbsolutePath(), String.format("--httpPort=%s", port)  
    });
    /*
    cl.addArguments(new String[] {
        getProgramArguments()
    });
    */
    command = new Command(cl.getArguments()).setDirectory(installationDirectory);
    //
    // One thread for the command being run
    // One thread for the processing of the command inputstream
    //
    ExecutorService executor = Executors.newFixedThreadPool(2);
    // Execute the command and let it run in the background
    command.execute(executor);

    System.out.println("Attempting to determine if Jenkins is ready!");    
    while (!readyToRespondToRequests()) {
      Thread.sleep(3000);
    }
  }

  public void stop() throws Exception {
    command.stop();
  }

  public String[] classpath() throws Exception {
    String pathSeparator = System.getProperty("path.separator");
    StringBuffer sb = new StringBuffer();
    for (String s : getClasspath()) {
      sb.append(s).append(pathSeparator);
    }
    return new String[] {
        sb.toString()
    };
  }

  public String[] getClasspath() throws Exception {
    DirectoryScanner ds = new DirectoryScanner();
    ds.setBasedir(installationDirectory);
    ds.setIncludes(new String[] {
        "lib/*.jar", "conf"
    });
    ds.scan();
    List<String> cp = new ArrayList<String>();
    for (String path : ds.getIncludedFiles()) {
      cp.add(path);
    }
    for (String path : ds.getIncludedDirectories()) {
      cp.add(path);
    }
    return cp.toArray(new String[cp.size()]);
  }

  public String[] getVMArguments() throws Exception {
    return new String[] {
        "-DJENKINS_HOME=" + quote(workDirectory)
    };
  }

  private String quote(File file) {
    return StringUtils.quoteAndEscape(file.getAbsolutePath(), '"');
  }

  private boolean readyToRespondToRequests() {
    HttpURLConnection connection = null;
    try {
      URL serverAddress = new URL(String.format("http://localhost:%s", port));
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
