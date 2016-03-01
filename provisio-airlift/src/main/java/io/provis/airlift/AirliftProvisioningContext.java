/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.airlift;

import java.io.File;
import java.util.List;

import com.google.common.collect.Lists;

public class AirliftProvisioningContext {
  private int port;
  private File serverHome;
  private String serverCoordinate;
  private String repositoryUrl;  
  private String statusUrl;
  
  public File getServerHome() {
    return serverHome;
  }

  public void setServerHome(File serverHome) {
    this.serverHome = serverHome;
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

  public String getServerCoordinate() {
    return serverCoordinate;
  }

  public void setServerCoordinate(String serverCoordinate) {
    this.serverCoordinate = serverCoordinate;
  }
  
  public String getStatusUrl() {
    return statusUrl;
  }

  public void setStatusUrl(String statusUrl) {
    this.statusUrl = statusUrl;
  }  
}
