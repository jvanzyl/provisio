/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

public class File {

  private String path;

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "File [path=" + path + "]";
  }
}
