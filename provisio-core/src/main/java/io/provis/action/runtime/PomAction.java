/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.action.runtime;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.Archiver.ArchiverBuilder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Named;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;

/**
 * Produces a POM that represents the dependencies of given runtime so that projects building
 * applications based on this runtime can easily access the dependencies.
 *
 * @author jvanzyl
 *
 */
public class PomAction implements ProvisioningAction {

  private String name;
  private String includes;
  private String excludes;
  private File runtimeDirectory;

  public void execute(ProvisioningContext context) {
    try {
      List<String> files = FileUtils.getFileNames(runtimeDirectory, includes, excludes, true);
      for (String file : files) {
        System.out.println(file);
      }
    } catch (IOException e) {
    }
  }
  
  private Dependency findDependencyFromFile(File file) {
    Dependency d = new Dependency();
    return d;
  }
  
}
