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

public interface ActionDescriptor {
  String getName();
  Class<?> getImplementation();
  String[] attributes();
  
  default List<Alias> aliases() { 
    return Lists.newArrayList();
  }
  
  default List<Implicit> implicits() { 
    return Lists.newArrayList();
  }
  
}
