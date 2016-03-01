package io.takari.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import io.airlift.http.server.WebServer;
import io.airlift.http.server.WebServerBuilder;

public class Server {

  public void run() throws Exception {
    File serverHome = new File(System.getProperty("user.dir"));
    Properties properties = new Properties();
    try (InputStream is = new FileInputStream(new File(serverHome, "etc/config.properties"))) {
      properties.load(is);
    }
    String port = properties.getProperty("http-server.http.port");
    WebServer server = new WebServerBuilder()
      .port(port != null ? Integer.parseInt(port) : 5000)
      .serve("/api/*").withJaxRs()
      .build();
    server.start();
  }

  public static void main(String[] args) throws Exception {
    Server server = new Server();
    server.run();
  }
}

