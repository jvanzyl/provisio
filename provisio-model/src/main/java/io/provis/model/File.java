/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

public class File {

  private String name;
  private String path;
  private String touch;
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getTouch() {
    return touch;
  }

  public void setTouch(String touch) {
    this.touch = touch;
  }

  @Override
  public String toString() {
    return "File [path=" + path + "]";
  }
}
