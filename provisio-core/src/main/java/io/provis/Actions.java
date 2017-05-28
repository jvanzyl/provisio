/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.provis.action.artifact.UnpackAction;
import io.provis.action.artifact.alter.AlterAction;
import io.provis.action.artifact.alter.Delete;
import io.provis.action.artifact.alter.Insert;
import io.provis.action.fileset.MakeExecutableAction;
import io.provis.action.runtime.ArchiveAction;
import io.provis.action.runtime.MakeDirectoryAction;
import io.provis.model.ActionDescriptor;
import io.provis.model.Alias;
import io.provis.model.Implicit;

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
            "filter", "mustache", "includes", "excludes", "flatten", "useRoot"
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
            "name", "executable", "prefix", "useRoot", "classifier"
        };
      }
    });

    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "mkdir";
      }

      @Override
      public Class<?> getImplementation() {
        return MakeDirectoryAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
            "name"
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
    
    actionDescriptors.add(new ActionDescriptor() {

      @Override
      public String getName() {
        return "alter";
      }

      @Override
      public Class<?> getImplementation() {
        return AlterAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {};
      }

      @Override
      public List<Alias> aliases() {
        return ImmutableList.of(new Alias("insert", Insert.class), new Alias("delete", Delete.class));
      }

      @Override
      public List<Implicit> implicits() {
        return ImmutableList.of(
            new Implicit("inserts", AlterAction.class, Insert.class), new Implicit("artifacts", Insert.class),
            new Implicit("deletes", AlterAction.class, Delete.class), new Implicit("files", Delete.class));
      }
    });    
    return actionDescriptors;
  }
}
