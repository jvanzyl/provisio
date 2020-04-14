/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.action.artifact.alter;

import static ca.vanzyl.provisio.ProvisioUtils.coordinateToPath;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.inject.Named;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.io.Files;

import ca.vanzyl.provisio.MavenProvisioner;
import ca.vanzyl.provisio.perms.PosixModes;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.UnArchiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The unpack is an operation that results in any number of artifacts and resources being contributed to the runtime. The archive to be unpacked can
 * make the metadata about its contents available, or we need to determine the information about the contents by examining the contents.
 *
 * @author jvanzyl
 *
 */
@Named("insert")
public class AlterAction implements ProvisioningAction {

  private static Logger logger = LoggerFactory.getLogger(AlterAction.class);

  private List<Insert> inserts;
  private List<Delete> deletes;
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
      if (inserts != null) {
        for (Insert insert : inserts) {
          for (ProvisioArtifact insertArtifact : insert.getArtifacts()) {
            provisioner.resolveArtifact(context, insertArtifact);
            File source = insertArtifact.getFile();
            File target = new File(unpackDirectory, insertArtifact.getName());
            Files.copy(source, target);
          }
        }
      }

      if(deletes != null) {
        for (Delete delete : deletes) {
          for (ca.vanzyl.provisio.model.File fileModel : delete.getFiles()) {
            logger.info("Deleting file {} from {}", fileModel.getPath(), artifact);
            File target = new File(unpackDirectory, fileModel.getPath());
            if(!target.exists()) {
              throw new RuntimeException("The file specified to delete does not exist: " + fileModel.getPath());
            }
            FileUtils.forceDelete(target);
          }
        }
      }

      // Set all the files readable so we can repack them
      setFilesReadable(unpackDirectory);
      // Pack the archive back up
      Archiver archiver = Archiver.builder()
        .useRoot(false)
        .build();
      String artifactName = artifact.getName() != null ? artifact.getName() : coordinateToPath(artifact);
      File alteredArtifact = new File(outputDirectory, artifactName);
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
    // noop on windows
    if (File.pathSeparatorChar == ';') return;
    java.nio.file.Files.setPosixFilePermissions(p, PosixModes.intModeToPosix(intMode));
  }

  public List<Insert> getInserts() {
    return inserts;
  }

  public void setInserts(List<Insert> inserts) {
    this.inserts = inserts;
  }

  public List<Delete> getDeletes() {
    return deletes;
  }

  public void setDeletes(List<Delete> deletes) {
    this.deletes = deletes;
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
