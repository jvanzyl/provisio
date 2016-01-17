/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

import io.provis.model.Runtime;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;

public class ProvisioningRequest {
  
  private File outputDirectory;
  private String localRepository;
  private Runtime model;
  private Map<String, String> versionMap;
  private List<String> managedDependencies = Collections.emptyList();
  private Map<String,String> variables;
  //
  private RepositorySystemSession repositorySystemSession;
  private List<RemoteRepository> remoteRepositories;
  
  public File getOutputDirectory() {
    return outputDirectory;
  }

  public ProvisioningRequest setOutputDirectory(File outputDirectory) {
    if (outputDirectory == null) {
      this.outputDirectory = new File("").getAbsoluteFile();
    } else {
      this.outputDirectory = outputDirectory.getAbsoluteFile();
    }
    return this;
  }

  public String getLocalRepository() {
    return localRepository;
  }

  public void setLocalRepository(String localRepository) {
    this.localRepository = localRepository;
  }

  public Runtime getRuntimeModel() {
    return model;
  }

  public ProvisioningRequest setModel(Runtime runtime) {
    this.model = runtime;
    return this;
  }
  
  //
  // VersionMap
  //
  public Map<String, String> getVersionMap() {
    return versionMap;
  }

  public ProvisioningRequest setVersionMap(Map<String, String> versionMap) {
    this.versionMap = versionMap;
    return this;
  }
  
  public void addVersionMap(Map<String, String> versionMap) {
    this.versionMap = versionMap;
  }

  public List<String> getManagedDependencies() {
    return managedDependencies;
  }

  public void setManagedDependencies(List<String> dependencyManagement) {
    this.managedDependencies = dependencyManagement;
  }

  public RepositorySystemSession getRepositorySystemSession() {
    return repositorySystemSession;
  }

  public void setRepositorySystemSession(RepositorySystemSession repositorySystemSession) {
    this.repositorySystemSession = repositorySystemSession;
  }

  public List<RemoteRepository> getRemoteRepositories() {
    return remoteRepositories;
  }

  public void setRemoteRepositories(List<RemoteRepository> remoteRepositories) {
    this.remoteRepositories = remoteRepositories;
  }

  public Runtime getRuntime() {
    return model;
  }

  public Map<String, String> getVariables() {
    return variables;
  }

  public void setVariables(Map<String, String> variables) {
    this.variables = variables;
  }  
  
  
}
