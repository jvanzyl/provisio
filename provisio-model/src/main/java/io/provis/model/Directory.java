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

public class Directory {

  private String path;
  private List<String> includes;
  private List<String> excludes;

  public String getPath() {
    return path;
  }

  public List<String> getIncludes() {
    if (includes == null) {
      includes = Lists.newArrayList();
    }
    return includes;
  }

  public List<String> getExcludes() {
    if (excludes == null) {
      excludes = Lists.newArrayList();
    }
    return excludes;
  }
}
