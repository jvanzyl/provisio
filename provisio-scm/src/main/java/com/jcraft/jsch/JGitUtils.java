package com.jcraft.jsch;

import java.io.File;
import java.util.Collection;

import org.eclipse.jgit.util.FS;

public class JGitUtils {

  public static boolean areAllKeysEncrypted() {
    final JSch jsch = new JSch();

    final File userHome = FS.DETECTED.userHome();
    if (userHome != null) {
      final File sshdir = new File(userHome, ".ssh");
      if (sshdir.isDirectory()) {
        loadIdentity(jsch, new File(sshdir, "identity"));
        loadIdentity(jsch, new File(sshdir, "id_rsa"));
        loadIdentity(jsch, new File(sshdir, "id_dsa"));
      }
    }

    @SuppressWarnings("unchecked")
    Collection<Identity> identities = (Collection<Identity>) jsch.identities;
    for (Identity identity : identities) {
      if (!identity.isEncrypted()) {
        return false;
      }
    }
    return !identities.isEmpty();
  }

  private static void loadIdentity(final JSch jsch, final File priv) {
    if (priv.isFile()) {
      try {
        jsch.addIdentity(priv.getAbsolutePath());
      } catch (JSchException e) {
        // Instead, pretend the key doesn't exist.
      }
    }
  }

}
