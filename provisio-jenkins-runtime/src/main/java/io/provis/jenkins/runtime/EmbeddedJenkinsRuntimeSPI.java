package io.provis.jenkins.runtime;

import java.io.File;

public class EmbeddedJenkinsRuntimeSPI implements JenkinsRuntimeSPI {

  @Override
  public JenkinsRuntime createRuntime(File rootDir, byte[] secretKey) {
    return new EmbeddedJenkinsRuntime(rootDir, secretKey);
  }

}
