package io.provis.jenkins;

import java.io.File;
import java.nio.charset.Charset;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.provis.jenkins.config.Configuration;

public class CLI {
  
  private static final Logger log = LoggerFactory.getLogger(CLI.class);
  
  @Option(name = "-r", metaVar = "localRepo", usage = "local repository")
  private File localRepo;
  
  @Option(name = "-t", metaVar = "templateDir", usage = "additional config templates")
  private File templates;
  
  @Argument(index = 0, required = true, metaVar = "configFile", usage = "configuration properties file")
  private File configuration;
  
  @Argument(index = 1, metaVar = "outputDir", usage = "output directory")
  private File outputDir;
  
  public static void main(String[] args) throws Exception {
    // force utf-8
    System.setProperty("file.encoding", "UTF-8");
    Charset.defaultCharset();
    
    CLI cli = new CLI();
    CmdLineParser cmd = new CmdLineParser(cli);
    try {
      cmd.parseArgument(args);
    } catch(CmdLineException e) {
      
      System.out.print("Usage: java -jar provisio-jenkins-uber-<v>.jar");
      cmd.printSingleLineUsage(System.out);
      System.out.println();
      
      e.printStackTrace();
      return;
    }
    cli.doMain();
  }
  
  private void doMain() throws Exception {
    if(localRepo == null) {
      localRepo = new File(new File(System.getProperty("user.home")), ".m2/repository");
    }
    
    if(outputDir == null) {
      outputDir = new File("");
    }
    
    log.info("Creating jenkins instance in " + outputDir.getAbsolutePath());
    
    JenkinsInstallationProvisioner p = new JenkinsInstallationProvisioner(localRepo);
    JenkinsInstallationContext ctx = new JenkinsInstallationContext(outputDir, new Configuration(configuration))
        .configOverrides(templates);
    p.provision(ctx);
  }
  
}

