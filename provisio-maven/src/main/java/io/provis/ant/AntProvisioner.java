package io.provis.ant;

import io.tesla.aether.internal.DefaultTeslaAether;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.UnArchiver;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.aether.resolution.ArtifactResolutionException;


@Named
public class AntProvisioner {

  @Inject
  private DefaultTeslaAether artifactResolver;

  private UnArchiver unarchiver;

  public AntProvisioner() {
    unarchiver = UnArchiver.builder()
      .useRoot(false)
      .flatten(false)
      .build();
  }
  
  public File provision(String antVersion, File installDir) throws IOException, ArtifactResolutionException {    
    if (antVersion == null || antVersion.length() <= 0) {
      throw new IllegalArgumentException("Ant version not specified");
    }
   
    File binZip = artifactResolver.resolveArtifact("org.apache.ant:apache-ant:zip:bin:" + antVersion).getArtifact().getFile();

    installDir.mkdirs();
    if (!installDir.isDirectory()) {
      throw new IllegalStateException("Could not create Ant install directory " + installDir);
    }

    unarchiver.unarchive(binZip, installDir);

    File ant = new File(installDir, "bin/ant");
    if (!ant.isFile()) {
      throw new IllegalStateException("Unpacking of Ant distro failed");
    }
    ant.setExecutable(true);

    return installDir;
  }
}
