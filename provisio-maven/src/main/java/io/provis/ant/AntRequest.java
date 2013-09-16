package io.provis.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AntRequest {

  private File antHome;
  private File workDir;
  private File buildXml;
  private Properties userProperties;
  private List<String> targets;

  public AntRequest() {
    workDir = new File("").getAbsoluteFile();
    buildXml = new File("build.xml");
  }

  public File getWorkDir() {
    return workDir;
  }

  public AntRequest setWorkDir(File workDir) {
    if (workDir == null) {
      this.workDir = new File("").getAbsoluteFile();
    } else {
      this.workDir = workDir;
    }
    return this;
  }

  public AntRequest setWorkDir(String workDir) {
    return setWorkDir((workDir != null) ? new File(workDir) : null);
  }

  public File getBuildXml() {
    return buildXml;
  }

  public AntRequest setBuildXml(File buildXml) {
    this.buildXml = buildXml;
    return this;
  }

  public AntRequest setPomFile(String pomFile) {
    return setBuildXml((pomFile != null) ? new File(pomFile) : null);
  }

  public Properties getUserProperties() {
    if (userProperties == null) {
      userProperties = new Properties();
    }
    return userProperties;
  }

  public AntRequest setUserProperties(Properties userProperties) {
    this.userProperties = userProperties;
    return this;
  }

  public AntRequest setUserProperty(String key, String value) {
    if (value == null) {
      getUserProperties().remove(key);
    } else {
      getUserProperties().setProperty(key, value);
    }
    return this;
  }

  public AntRequest addUserProperties(Map<?, ?> properties) {
    if (properties != null) {
      for (Map.Entry<?, ?> entry : properties.entrySet()) {
        if (entry.getValue() == null) {
          getUserProperties().remove(entry.getKey());
        } else if (entry.getKey() != null) {
          getUserProperties().setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
      }
    }
    return this;
  }

  public List<String> getTargets() {
    if (targets == null) {
      targets = new ArrayList<String>();
    }
    return targets;
  }

  public AntRequest setTargets(List<String> goals) {
    this.targets = goals;
    return this;    
  }

  public AntRequest setTargets(String... goals) {
    this.targets = new ArrayList<String>(Arrays.asList(goals));
    return this;
  }

  public AntRequest addTargets(String... goals) {
    Collections.addAll(getTargets(), goals);
    return this;
  }

  public File getAntHome() {
    return antHome;
  }

  public AntRequest setAntHome(File mavenHome) {
    this.antHome = mavenHome;
    return this;
  }
}
