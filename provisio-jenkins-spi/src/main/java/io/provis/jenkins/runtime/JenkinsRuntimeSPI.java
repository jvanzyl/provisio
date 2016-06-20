package io.provis.jenkins.runtime;

import java.io.File;

public interface JenkinsRuntimeSPI {
  JenkinsRuntime createRuntime(File rootDir, byte[] secretKey);
}
