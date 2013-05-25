package com.sonatype.provisio.scm;

import org.codehaus.plexus.util.Os;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

class JgitSystemReader extends SystemReader {

  private static final String GIT_SSH = "GIT_SSH";

  private final SystemReader delegate;

  private final String gitSsh;

  public static void install() {
    if (System.getenv(GIT_SSH) == null) {
      if (Os.isFamily(Os.FAMILY_UNIX)) {
        SystemReader.setInstance(new JgitSystemReader(SystemReader.getInstance(), "ssh"));
      } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        SystemReader.setInstance(new JgitSystemReader(SystemReader.getInstance(), "tortoiseplink"));
      }
    }
  }

  private JgitSystemReader(SystemReader delegate, String gitSsh) {
    this.delegate = delegate;
    this.gitSsh = gitSsh;
  }

  @Override
  public String getHostname() {
    return delegate.getHostname();
  }

  @Override
  public String getenv(String variable) {
    if (GIT_SSH.equals(variable)) {
      return gitSsh;
    }
    return delegate.getenv(variable);
  }

  @Override
  public String getProperty(String key) {
    return delegate.getProperty(key);
  }

  @Override
  public FileBasedConfig openUserConfig(Config parent, FS fs) {
    return delegate.openUserConfig(parent, fs);
  }

  @Override
  public FileBasedConfig openSystemConfig(Config parent, FS fs) {
    return delegate.openSystemConfig(parent, fs);
  }

  @Override
  public long getCurrentTime() {
    return delegate.getCurrentTime();
  }

  @Override
  public int getTimezone(long when) {
    return delegate.getTimezone(when);
  }

}
