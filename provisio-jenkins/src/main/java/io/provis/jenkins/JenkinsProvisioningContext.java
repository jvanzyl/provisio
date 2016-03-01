/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.jenkins;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;

public class JenkinsProvisioningContext {
  private String version;
  private File installationDirectory;
  private File workDirectory;
  private List<String> pluginRepositories;
  private List<String> plugins;
  private String repositoryUrl;  
  private int port = 8080;

  public JenkinsProvisioningContext() {
    this.plugins = Lists.newArrayList();
    this.pluginRepositories = Lists.newArrayList();
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public File getInstallationDirectory() {
    return installationDirectory;
  }

  public void setInstallationDirectory(File installationDirectory) {
    this.installationDirectory = installationDirectory;
  }

  public File getWorkDirectory() {
    return workDirectory;
  }

  public void setWorkDirectory(File workDirectory) {
    this.workDirectory = workDirectory;
  }

  public List<String> getPluginRepositories() {
    return pluginRepositories;
  }

  public void addPluginRepository(String pluginRepository) {
    pluginRepositories.add(pluginRepository);
  }

  public List<String> getPlugins() {
    return plugins;
  }

  public void addPlugin(String plugin) {
    plugins.add(plugin);
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getRepositoryUrl() {
    return repositoryUrl;
  }

  public void setRepositoryUrl(String repositoryUrl) {
    this.repositoryUrl = repositoryUrl;
  }    
}
