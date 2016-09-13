package io.provis.jenkins.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import io.airlift.http.server.WebServer;
import io.airlift.http.server.WebServerBuilder;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Jenkins {

  public static void main(String[] args) throws Exception {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
    
    // Retrieve working directory
    File workingDirectory = new File("").getAbsoluteFile();
    
    // Use config.properties
    File configPropertiesFile = new File(workingDirectory, "etc/config.properties");
    Properties configProperties = new Properties();
    try(InputStream is = new FileInputStream(configPropertiesFile)) {
      configProperties.load(is);
    }    
    int port = Integer.parseInt(configProperties.getProperty("jenkins.http.port", "8080"));
    // Locate jenkins webapp
    File webapp = new File(workingDirectory, "jenkins");
    WebServer webServer = new WebServerBuilder()
      .port(port)
      .serve("/").with(webapp, JenkinsHelper.securityHandler())
      .build();
    webServer.start();
  }
}
