/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.model;

import java.util.List;

import com.google.common.collect.Lists;

public class FileSet {

  private String to;
  private List<File> files;
  private List<Directory> directories;

  public String getDirectory() {
    return to;
  }

  public void setDirectory(String to) {
    this.to = to;
  }

  public List<File> getFiles() {
    if (files == null) {
      files = Lists.newArrayList();
    }
    return files;
  }

  public List<Directory> getDirectories() {
    if (directories == null) {
      directories = Lists.newArrayList();
    }
    return directories;
  }

  @Override
  public String toString() {
    return "FileSet [directory=" + to + ", files=" + files + ", directories=" + directories + "]";
  }

}
