package com.sonatype.provisio.scm;

import java.io.File;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.repository.ScmRepository;

class MavenScmRepository implements com.sonatype.provisio.scm.ScmRepository {

  private final File basedir;

  private final ScmProvider provider;

  private final ScmRepository repo;

  public MavenScmRepository(ScmProvider provider, ScmRepository repo, File basedir) {
    this.provider = provider;
    this.repo = repo;
    this.basedir = basedir;
  }

  public File getBasedir() {
    return basedir;
  }

  public void update() {
    try {
      provider.update(repo, new ScmFileSet(basedir));
    } catch (ScmException e) {
      throw new IllegalStateException(e);
    }
  }

  public void commit(String message) {
    try {
      provider.checkIn(repo, new ScmFileSet(basedir), message);
    } catch (ScmException e) {
      throw new IllegalStateException(e);
    }
  }

  public void close() {
    // noop
  }

}
