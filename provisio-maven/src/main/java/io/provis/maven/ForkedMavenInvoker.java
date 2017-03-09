/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.maven;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.inject.Named;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

@Named("forked")
public class ForkedMavenInvoker implements MavenInvoker {

  public MavenResult invoke(MavenRequest request) {
    MavenResult result = new MavenResult();

    File javaHome = new File(System.getProperty("java.home")).getAbsoluteFile();
    if (javaHome.getName().equals("jre")) {
      javaHome = javaHome.getParentFile();
    }

    File mavenHome = request.getMavenHome();
    Commandline cli = new Commandline();

    File exec;
    if (File.pathSeparatorChar == ';') {
      exec = new File(mavenHome, "bin/mvn.cmd");
      if (!exec.exists()) {
        exec = new File(mavenHome, "bin/mvn.bat");
      }
    } else {
      exec = new File(mavenHome, "bin/mvn");
    }

    cli.setExecutable(exec.getAbsolutePath());

    cli.addEnvironment("M2_HOME", mavenHome.getAbsolutePath());
    cli.addEnvironment("JAVA_HOME", javaHome.getAbsolutePath());
    cli.addEnvironment("MAVEN_TERMINATE_CMD", "on");

    cli.setWorkingDirectory(request.getWorkDir());

    cli.createArg().setValue("-e");
    cli.createArg().setValue("-B");

    if (request.getPomFile() != null) {
      cli.createArg().setValue("-f");
      cli.createArg().setValue(request.getPomFile().getPath());
    }

    for (Map.Entry<Object, Object> entry : request.getUserProperties().entrySet()) {
      cli.createArg().setValue("-D");
      cli.createArg().setValue(entry.getKey() + "=" + entry.getValue());
    }

    if (request.getLocalRepo() != null) {
      cli.createArg().setValue("-D");
      cli.createArg().setValue("maven.repo.local=" + request.getLocalRepo().getAbsolutePath());
    }

    if (request.getGlobalSettings() != null) {
      cli.createArg().setValue("-gs");
      cli.createArg().setFile(request.getGlobalSettings());
    }

    if (request.getUserSettings() != null) {
      cli.createArg().setValue("-s");
      cli.createArg().setFile(request.getUserSettings());
    }

    for (String goal : request.getGoals()) {
      cli.createArg().setValue(goal);
    }

    StringWriter sw = new StringWriter(128 * 1024);

    try {
      MavenStreamConsumer consumer = new MavenStreamConsumer(new PrintWriter(sw));
      int exitCode = CommandLineUtils.executeCommandLine(cli, consumer, consumer);
      result.setOutput(sw.toString());
      System.out.println(sw);
      if (exitCode != 0) {
        throw new CommandLineException("Maven invocation failed with exit code " + exitCode);
      }
    } catch (CommandLineException e) {
      result.getErrors().add(e);
    }

    return result;
  }

  private class MavenStreamConsumer implements StreamConsumer {

    private final PrintWriter pw;

    public MavenStreamConsumer(PrintWriter pw) {
      this.pw = pw;
    }

    public void consumeLine(String line) {
      pw.println(line);
    }
  }

}
