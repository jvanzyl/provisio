package com.sonatype.provisio.scm;

import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

class JgitUserInfo implements UserInfo {

  private final URIish uri;

  private final CredentialsProvider credProvider;

  private char[] password;

  private char[] passphrase;

  public JgitUserInfo(Session session, CredentialsProvider credProvider) {
    this.uri = createURI(session);
    this.credProvider = credProvider;
  }

  private static URIish createURI(Session session) {
    URIish uri = new URIish();
    uri = uri.setScheme("ssh");
    uri = uri.setUser(session.getUserName());
    uri = uri.setHost(session.getHost());
    uri = uri.setPort(session.getPort());
    return uri;
  }

  public String getPassphrase() {
    return (passphrase != null) ? new String(passphrase) : null;
  }

  public String getPassword() {
    return (password != null) ? new String(password) : null;
  }

  public boolean promptPassword(String message) {
    CredentialItem.Password pwd = new CredentialItem.Password();
    if (credProvider.supports(pwd) && credProvider.get(uri, pwd)) {
      password = pwd.getValue();
      return true;
    } else {
      password = null;
      return false;
    }
  }

  public boolean promptPassphrase(String message) {
    JgitCredentialsProvider.Passphrase phrase = new JgitCredentialsProvider.Passphrase();
    if (credProvider.supports(phrase) && credProvider.get(uri, phrase)) {
      passphrase = phrase.getValue();
      return true;
    } else {
      passphrase = null;
      return false;
    }
  }

  public boolean promptYesNo(String message) {
    if (message.contains("authenticity of host")) {
      return true;
    }
    return false;
  }

  public void showMessage(String message) {
  }

}
