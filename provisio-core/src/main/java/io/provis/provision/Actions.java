package io.provis.provision;

import io.provis.model.ActionDescriptor;
import io.provis.provision.action.artifact.UnpackAction;
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
    return actionDescriptors;
  }
}
