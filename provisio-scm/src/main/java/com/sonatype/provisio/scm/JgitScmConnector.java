package com.sonatype.provisio.scm;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;

import com.jcraft.jsch.JGitUtils;
import com.jcraft.jsch.JSchException;

@Named("jgit")
public class JgitScmConnector implements ScmConnector {

  static {
    JgitSessionFactory.install();
  }

  @Inject
  private Logger log;

  private String sshPassword;

  private String sshPassphrase;

  private static final String SCM_PREFIX = "scm:git:";

  @Inject
  @SuppressWarnings("unused")
  private void setSshPassword(@Named("${ssh.password:-}") String password) {
    this.sshPassword = (password == null || password.length() <= 0) ? null : password;
  }

  @Inject
  @SuppressWarnings("unused")
  private void setSshPassphrase(@Named("${ssh.passphrase:-}") String passphrase) {
    /*
     * Alternatively, the env var GIT_SSH can be set to point at an external ssh cmd which usually can pick up the passphrase from some OS-level keychain.
     */
    this.sshPassphrase = (passphrase == null || passphrase.length() <= 0) ? null : passphrase;

    if (this.sshPassphrase == null && JGitUtils.areAllKeysEncrypted()) {
      log.info("All SSH keys are encrypted but system property ssh.passphrase is not set, falling back to external SSH client");
      JgitSystemReader.install();
    }
  }

  public ScmRepository checkout(String uri, File directory) {
    uri = parseUri(uri);

    log.info("Checking out master branch of {} to {}", uri, directory);

    JgitCredentialsProvider credentialsProvider = new JgitCredentialsProvider(sshPassword, sshPassphrase);

    CloneCommand clone = Git.cloneRepository();
    clone.setURI(uri).setDirectory(directory).setBranch("refs/heads/master");
    clone.setCredentialsProvider(credentialsProvider);
    Git git;
    try {
      git = clone.call();
    } catch (JGitInternalException e) {
      if (e.getCause() instanceof TransportException && e.getCause().getCause() instanceof JSchException && e.getCause().getCause().toString().contains("Auth")) {
        throw new JGitInternalException("Could not clone Git repo " + e.getCause().getMessage() + ", if your private key is encrypted, try setting the system property ssh.passphrase"
            + " or alternatively set the environment variable GIT_SSH to your ssh executable", e.getCause());
      }
      throw e;
    }

    StoredConfig config = git.getRepository().getConfig();
    try {
      String remoteName = Constants.DEFAULT_REMOTE_NAME;
      String branchName = Constants.MASTER;
      config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
      config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
      config.save();
    } catch (IOException e) {
      throw new JGitInternalException(e.getMessage(), e);
    }

    return new JgitScmRepository(git, credentialsProvider);
  }

  private String parseUri(String uri) {
    if (uri.startsWith(SCM_PREFIX)) {
      uri = uri.substring(SCM_PREFIX.length());
    } else if (ScmUriUtils.GIT.equals(ScmUriUtils.guessScmType(uri))) {
      // very likely git
    } else {
      throw new UnsupportedScmException(uri);
    }

    URIish gitUri;
    try {
      gitUri = new URIish(uri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }

    if (gitUri.getScheme() == null) {
      if (gitUri.getHost() == null || "file".equals(gitUri.getHost())) {
        gitUri = gitUri.setHost(null);
        gitUri = gitUri.setScheme("file");
      } else {
        gitUri = gitUri.setScheme("ssh");
      }
    }

    uri = gitUri.toString();

    return uri;
  }

}
