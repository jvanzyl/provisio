package com.sonatype.provisio.scm;

import io.tesla.proviso.spi.AbstractProvisioningTask;
import io.tesla.proviso.spi.ProvisioningContext;
import io.tesla.proviso.util.IO;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;


@Named(CodebaseTask.ID)
public class CodebaseTask extends AbstractProvisioningTask {

  static final String ID = "codebase";

  @Inject
  private ScmManager scmManager;

  @Inject
  private IO io;

  public String getId() {
    return ID;
  }

  @Override
  public void preStart(ProvisioningContext context) {
    String uri = context.getConfiguration().get("", ID + ".uri");
    if (uri.length() <= 0) {
      return;
    }

    String dirname = guessDirectory(uri);

    File checkoutDir = context.resolvePath(dirname);

    io.delete(checkoutDir);

    scmManager.checkout(uri, checkoutDir).close();
  }

  private static String guessDirectory(String uri) {
    String directory = "checkout";

    if (uri.indexOf('/') > 0) {
      directory = uri.substring(uri.lastIndexOf('/') + 1);

      if ("trunk".equals(directory)) {
        int s1 = uri.lastIndexOf('/');
        int s2 = uri.lastIndexOf('/', s1 - 1);
        if (s2 > 0) {
          directory = uri.substring(s2 + 1, s1);
        }
      } else if (directory.indexOf('.') > 0) {
        directory = directory.substring(0, directory.lastIndexOf('.'));
      }
    }

    return directory;
  }

}
