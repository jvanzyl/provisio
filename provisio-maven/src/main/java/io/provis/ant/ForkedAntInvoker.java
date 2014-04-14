package io.provis.ant;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.inject.Named;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;

//
// This fixes the specification of properties on the command line, but the newer version of tesla-aether is causing problems with the
// HostNameVerifier since HeartBleed and I'll sort it out later
//
@Named("forked")
public class ForkedAntInvoker implements AntInvoker {

  public AntResult invoke(AntRequest request) {
    AntResult result = new AntResult();

    File javaHome = new File(System.getProperty("java.home")).getAbsoluteFile();
    if (javaHome.getName().equals("jre")) {
      javaHome = javaHome.getParentFile();
    }

    File antHome = request.getAntHome();
    Commandline cli = new Commandline();

    cli.setExecutable(new File(antHome, "bin/ant").getAbsolutePath());

    cli.addEnvironment("ANT_HOME", antHome.getAbsolutePath());
    cli.addEnvironment("JAVA_HOME", javaHome.getAbsolutePath());

    cli.setWorkingDirectory(request.getWorkDir());

    cli.createArg().setValue("-d");

    if (request.getBuildXml() != null) {
      cli.createArg().setValue("-f");
      cli.createArg().setValue(request.getBuildXml().getPath());
    }

    for (Map.Entry<Object, Object> entry : request.getUserProperties().entrySet()) {
      String value = (String) entry.getValue();
      cli.createArg().setValue(String.format("-D%s=%s", entry.getKey(), value));
    }

    for (String goal : request.getTargets()) {
      cli.createArg().setValue(goal);
    }

    StringWriter sw = new StringWriter(128 * 1024);

    try {
      AntStreamConsumer consumer = new AntStreamConsumer(new PrintWriter(sw));
      System.out.println(cli);
      int exitCode = CommandLineUtils.executeCommandLine(cli, consumer, consumer);
      result.setOutput(sw.toString());
      System.out.println(sw);
      if (exitCode != 0) {
        throw new CommandLineException("Ant invocation failed with exit code " + exitCode);
      }
    } catch (CommandLineException e) {
      result.getErrors().add(e);
    }

    return result;
  }

  private class AntStreamConsumer implements StreamConsumer {

    private final PrintWriter pw;

    public AntStreamConsumer(PrintWriter pw) {
      this.pw = pw;
    }

    public void consumeLine(String line) {
      pw.println(line);
    }
  }

}
