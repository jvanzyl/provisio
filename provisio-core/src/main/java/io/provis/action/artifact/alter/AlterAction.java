/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis.action.artifact.alter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import io.provis.MavenProvisioner;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.perms.PosixModes;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.UnArchiver;

/**
 * The unpack is an operation that results in any number of artifacts and resources being contributed to the runtime. The archive to be unpacked can
 * make the metadata about its contents available, or we need to determine the information about the contents by examining the contents.
 * 
 * @author jvanzyl
 *
 */
@Named("insert")
public class AlterAction implements ProvisioningAction {
  private List<Insert> inserts;
  private ProvisioArtifact artifact;
  private File outputDirectory;
  private MavenProvisioner provisioner;

  @Override
  public void execute(ProvisioningContext context) {
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }
    File archive = artifact.getFile();
    try {
      // Unpack the artifact in question
      UnArchiver unarchiver = UnArchiver.builder()
        .build();
      File unpackDirectory = new File(outputDirectory, "unpack");
      unarchiver.unarchive(archive, unpackDirectory);
      // Make any modifications to the archive
      for (Insert insert : inserts) {
        for (ProvisioArtifact insertArtifact : insert.getArtifacts()) {
          provisioner.resolveArtifact(context, insertArtifact);
          File source = insertArtifact.getFile();
          File target = new File(unpackDirectory, insertArtifact.getName());
          Files.copy(source, target);
        }
      }
      // Set all the files readable so we can repack them
      setFilesReadable(unpackDirectory);
      // Pack the archive back up      
      Archiver archiver = Archiver.builder()
        .useRoot(false)
        .build();
      File alteredArtifact = new File(outputDirectory, artifact.getName());
      archiver.archive(alteredArtifact, unpackDirectory);
      FileUtils.deleteDirectory(unpackDirectory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void setFilesReadable(File directory) throws IOException {
    java.nio.file.Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
        setPermissionsOn(path, 0755);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
        setPermissionsOn(path, 0755);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  void setPermissionsOn(Path p, int intMode) throws IOException {
    java.nio.file.Files.setPosixFilePermissions(p, PosixModes.intModeToPosix(intMode));
  }

  public List<Insert> getInserts() {
    return inserts;
  }

  public void setInserts(List<Insert> inserts) {
    this.inserts = inserts;
  }

  public ProvisioArtifact getArtifact() {
    return artifact;
  }

  public void setArtifact(ProvisioArtifact artifact) {
    this.artifact = artifact;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public MavenProvisioner getProvisioner() {
    return provisioner;
  }

  public void setProvisioner(MavenProvisioner provisioner) {
    this.provisioner = provisioner;
  }
}
