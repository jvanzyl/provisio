/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.nexus;

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

public class NexusForkedLauncher {

  private File nexusInstallationDirectory;
  private File nexusWorkDirectory;
  private int nexusPort;
  private Commandline cl;
  private Command command;

  public NexusForkedLauncher(NexusProvisioningContext context) throws Exception {
    this.nexusInstallationDirectory = context.getInstallationDirectory();
    this.nexusWorkDirectory = context.getWorkDirectory();
    this.nexusPort = context.getPort();
  }

  public void start() throws Exception {
    //
    // Create the work directory as it's expected by Nexus. You get something like this otherwise:
    // java.io.IOException: File ${nexusWorkDirectory}/access/ips.db.h2.db does not exist
    //
    FileUtils.mkdir(nexusWorkDirectory.getAbsolutePath());
    //
    // java $vmArgs -cp ${classPath} ${mainClass} ${programArguments}
    //
    cl = new Commandline();
    cl.setWorkingDirectory(nexusInstallationDirectory);
    cl.addArguments(new String[] {
      "java", "-Xms256m", "-Xmx1024m", "-XX:PermSize=1024m", "-XX:MaxPermSize=1024m",
    });    
    
    cl.addArguments(getVMArguments());
    cl.addArguments(new String[] {
      "-cp"
    });
    cl.addArguments(classpath());
    cl.addArguments(new String[] {
      getMainClass()
    });
    cl.addArguments(new String[] {
      getProgramArguments()
    });

    command = new Command(cl.getArguments()).setDirectory(nexusInstallationDirectory);
    //
    // One thread for the command being run
    // One thread for the processing of the command inputstream
    //
    ExecutorService executor = Executors.newFixedThreadPool(2);
    //
    // Execute the command and let it run in the background
    //
    command.execute(executor);

    //
    //
    // http://localhost:8081/nexus/service/local/status" 
    //
    System.out.println("We are waiting for Nexus to be ready!!!!");
    while (!readyToRespondToRequests()) {
      Thread.sleep(3000);
    }
  }

  public void stop() throws Exception {
    //
    // This unfortunately does not stop the sub-process that JSW creates. Eclipse launching must find child processes and kill them. We
    // appear to need to kill the process we started, and use the Launcher to stop the Java process that it created.
    //
    command.stop(); // main JSW process
    new Launcher().commandStop(); // JVM sub-process
  }

  public String getMainClass() throws Exception {
    return "org.sonatype.nexus.bootstrap.Launcher";
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
    ds.setBasedir(nexusInstallationDirectory);
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

  public String getProgramArguments() throws Exception {
    return "./conf/jetty.xml";
  }

  public String[] getVMArguments() throws Exception {
    return new String[] {
        "-Dnexus.nexus-work=" + quote(nexusWorkDirectory), "-Djetty.application-port=" + nexusPort + ""
    };
  }

  private String quote(File file) {
    return StringUtils.quoteAndEscape(file.getAbsolutePath(), '"');
  }

  private boolean readyToRespondToRequests() {
    HttpURLConnection connection = null;
    try {
      URL serverAddress = new URL(String.format("http://localhost:%s/nexus/service/local/status", nexusPort));
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
