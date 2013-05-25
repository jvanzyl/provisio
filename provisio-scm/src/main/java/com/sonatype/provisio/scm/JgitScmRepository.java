package com.sonatype.provisio.scm;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;

class JgitScmRepository implements ScmRepository {

  private final Git git;

  private final CredentialsProvider credentialsProvider;

  public JgitScmRepository(Git git, CredentialsProvider credentialsProvider) {
    this.git = git;
    this.credentialsProvider = credentialsProvider;
  }

  public File getBasedir() {
    return git.getRepository().getDirectory().getParentFile();
  }

  public void update() {
    try {
      git.pull().setCredentialsProvider(credentialsProvider).call();
    } catch (GitAPIException e) {
      throw new IllegalStateException(e);
    }
  }

  public void commit(String message) {
    try {
      git.commit().setMessage(message).setAll(true).call();
      git.push().setCredentialsProvider(credentialsProvider).setTimeout(15).call();
    } catch (GitAPIException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public void close() {
    git.getRepository().close();
  }

}
