package io.provis.ant;

import io.provis.SimpleProvisioner;
import io.tesla.proviso.archive.UnArchiver;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

@Named
public class AntProvisioner extends SimpleProvisioner {

  private UnArchiver unarchiver;

  public AntProvisioner() {
    unarchiver = UnArchiver.builder().useRoot(false).flatten(false).build();
  }

  public File provision(String antVersion, File installDir) throws IOException {
    if (antVersion == null || antVersion.length() <= 0) {
      throw new IllegalArgumentException("Ant version not specified");
    }

    File binZip = resolveFromServer(String.format("http://archive.apache.org/dist/ant/binaries/apache-ant-%s-bin.zip", antVersion), "org.apache.ant:apache-ant:zip:bin:" + antVersion);

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
