package com.sonatype.provisio.scm;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.SshConfigSessionFactory;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class JgitSessionFactory extends SshConfigSessionFactory {

  public static void install() {
    SshSessionFactory.setInstance(new JgitSessionFactory());
  }

  protected void configure(Host hc, Session session) {
  }

  @Override
  public synchronized Session getSession(String user, String pass, String host, int port, CredentialsProvider credentialsProvider, FS fs) throws JSchException {
    Session session = super.getSession(user, pass, host, port, credentialsProvider, fs);
    if (credentialsProvider != null) {
      /*
       * JGit's adapter for UserInfo is pretty useless as it doesn't allow the CredentialProvider to distinguish what credential string needs to be provided.
       */
      session.setUserInfo(new JgitUserInfo(session, credentialsProvider));
    }
    return session;
  }

}
