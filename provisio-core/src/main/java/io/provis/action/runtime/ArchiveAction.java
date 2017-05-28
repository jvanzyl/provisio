/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.action.runtime;

import org.codehaus.plexus.util.StringUtils;

import java.io.File;

import io.provis.model.Archive;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.Archiver.ArchiverBuilder;

public class ArchiveAction implements ProvisioningAction {

  private String name;
  private String prefix;
  private boolean useRoot;
  private String classifier;
  private String executable;
  private File runtimeDirectory;

  public void execute(ProvisioningContext context) {
    ArchiverBuilder builder = Archiver.builder();
    if (executable != null) {
      builder.executable(StringUtils.split(executable, ","));
    }
    Archiver archiver = builder
      .useRoot(useRoot)
      .withPrefix(prefix)
      .posixLongFileMode(true)
      .build();
    try {
      File archiveFile;
      if (name.startsWith(File.separator)) {
        archiveFile = new File(name);
      } else {
        archiveFile = new File(new File(runtimeDirectory, "../").getCanonicalFile(), name);
      }
      archiver.archive(archiveFile, runtimeDirectory);
      //
      // Right now this action has some special meaning it maybe shouldn't, but we need to know what archives are produced
      // so that we can set/attach the artifacts in a MavenProject.
      //
      Archive archive = new Archive(archiveFile);
      if(classifier != null) {
        archive.setClassifier(classifier);
      }
      context.getResult().addArchive(archive);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public boolean isUseRoot() {
    return useRoot;
  }

  public void setUseRoot(boolean useRoot) {
    this.useRoot = useRoot;
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }
}
