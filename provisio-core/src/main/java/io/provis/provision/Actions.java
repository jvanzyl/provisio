/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.provision;

import io.provis.model.ActionDescriptor;
import io.provis.provision.action.artifact.UnpackAction;
import io.provis.provision.action.fileset.MakeExecutableAction;
import io.provis.provision.action.runtime.ArchiveAction;

import java.util.List;

import com.google.common.collect.Lists;

public class Actions {

  public static List<ActionDescriptor> defaultActionDescriptors() {
    List<ActionDescriptor> actionDescriptors = Lists.newArrayList();
    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "unpack";
      }

      @Override
      public Class<?> getImplementation() {
        return UnpackAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
            "filter", "includes", "excludes", "flatten", "useRoot"
        };
      }
    });
    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "archive";
      }

      @Override
      public Class<?> getImplementation() {
        return ArchiveAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
            "name", "executable"
        };
      }
    });
    actionDescriptors.add(new ActionDescriptor() {

      @Override
      public String getName() {
        return "executable";
      }

      @Override
      public Class<?> getImplementation() {
        return MakeExecutableAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
            "includes", "excludes"
        };
      }

    });
    return actionDescriptors;
  }
}
