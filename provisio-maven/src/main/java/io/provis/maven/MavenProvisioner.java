package io.provis.maven;

import io.provis.Provisioner;
import io.tesla.proviso.archive.UnArchiver;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

@Named
public class MavenProvisioner extends Provisioner {

  private UnArchiver unarchiver;

  public MavenProvisioner() {
    unarchiver = UnArchiver.builder().useRoot(false).flatten(false).build();
  }

  public File provision(String mavenVersion, File installDirectory) throws IOException {
    if (mavenVersion == null || mavenVersion.length() <= 0) {
      throw new IllegalArgumentException("Maven version not specified");
    }

    File archive = resolveFromRepository("http://repo1.maven.org/maven2", "org.apache.maven:apache-maven:zip:bin:" + mavenVersion);

    installDirectory.mkdirs();
    if (!installDirectory.isDirectory()) {
      throw new IllegalStateException("Could not create Maven install directory " + installDirectory);
    }

    unarchiver.unarchive(archive, installDirectory);

    File mvn = new File(installDirectory, "bin/mvn");
    if (!mvn.isFile()) {
      throw new IllegalStateException("Unpacking of Maven distro failed");
    }
    mvn.setExecutable(true);

    return installDirectory;
  }
}
