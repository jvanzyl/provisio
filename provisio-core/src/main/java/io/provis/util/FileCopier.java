package io.provis.util;

import io.provis.provision.ProvisioningException;

import java.io.File;
import java.io.IOException;

import com.google.common.io.Files;

public class FileCopier {
  public static void copy(File source, File target) {
    try {
      if(target.getParentFile().exists() == false) {
        target.getParentFile().mkdirs();
      }
      Files.copy(source, target);
    } catch (IOException e) {
      throw new ProvisioningException("Error copying " + source + " to " + target, e);
    }
  }
}
