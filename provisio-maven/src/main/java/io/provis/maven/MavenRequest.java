package io.provis.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MavenRequest {

  private File mavenHome;
  private File workDir;
  private File pomFile;
  private File globalSettings;
  private File userSettings;
  private File localRepo;
  private Properties userProperties;
  private List<String> goals;

  public MavenRequest() {
    workDir = new File("").getAbsoluteFile();
    File f = new File(System.getProperty("user.home"), ".m2/settings.xml");
    if (f.exists()) {
      userSettings = f;
    }
    pomFile = new File("pom.xml");
    String mlr = System.getProperty("maven.repo.local", "");
    if (mlr.length() > 0) {
      localRepo = new File(mlr).getAbsoluteFile();
    }
  }

  public File getWorkDir() {
    return workDir;
  }

  public MavenRequest setWorkDir(File workDir) {
    if (workDir == null) {
      this.workDir = new File("").getAbsoluteFile();
    } else {
      this.workDir = workDir;
    }
    return this;
  }

  public MavenRequest setWorkDir(String workDir) {
    return setWorkDir((workDir != null) ? new File(workDir) : null);
  }

  public File getPomFile() {
    return pomFile;
  }

  public MavenRequest setPomFile(File pomFile) {
    this.pomFile = pomFile;
    return this;
  }

  public MavenRequest setPomFile(String pomFile) {
    return setPomFile((pomFile != null) ? new File(pomFile) : null);
  }

  public File getUserSettings() {
    return userSettings;
  }

  public MavenRequest setUserSettings(File userSettings) {
    this.userSettings = userSettings;
    return this;
  }

  public File getGlobalSettings() {
    return globalSettings;
  }

  public MavenRequest setGlobalSettings(File globalSettings) {
    this.globalSettings = globalSettings;
    return this;
  }

  public File getLocalRepo() {
    return localRepo;
  }

  public MavenRequest setLocalRepo(File localRepo) {
    this.localRepo = localRepo;
    return this;
  }

  public Properties getUserProperties() {
    if (userProperties == null) {
      userProperties = new Properties();
    }
    return userProperties;
  }

  public MavenRequest setUserProperties(Properties userProperties) {
    this.userProperties = userProperties;
    return this;
  }

  public MavenRequest setUserProperty(String key, String value) {
    if (value == null) {
      getUserProperties().remove(key);
    } else {
      getUserProperties().setProperty(key, value);
    }
    return this;
  }

  public MavenRequest addUserProperties(Map<?, ?> properties) {
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

  public List<String> getGoals() {
    if (goals == null) {
      goals = new ArrayList<String>();
    }
    return goals;
  }

  public MavenRequest setGoals(List<String> goals) {
    this.goals = goals;
    return this;
  }

  public MavenRequest setGoals(String... goals) {
    this.goals = new ArrayList<String>(Arrays.asList(goals));
    return this;
  }

  public MavenRequest addGoals(String... goals) {
    Collections.addAll(getGoals(), goals);
    return this;
  }

  public File getMavenHome() {
    return mavenHome;
  }

  public MavenRequest setMavenHome(File mavenHome) {
    this.mavenHome = mavenHome;
    return this;
  }
}
