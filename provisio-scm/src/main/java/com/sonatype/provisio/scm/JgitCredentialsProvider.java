package com.sonatype.provisio.scm;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialItem.CharArrayType;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class JgitCredentialsProvider extends CredentialsProvider {

  public static class Passphrase extends CharArrayType {
    public Passphrase() {
      super("Passphrase", true);
    }
  }

  private final char[] password;

  private final char[] passphrase;

  public JgitCredentialsProvider(String password, String passphrase) {
    this.password = (password != null) ? password.toCharArray() : null;
    this.passphrase = (passphrase != null) ? passphrase.toCharArray() : null;
  }

  public boolean isInteractive() {
    return false;
  }

  public boolean supports(CredentialItem... items) {
    for (CredentialItem item : items) {
      if (item instanceof JgitCredentialsProvider.Passphrase) {
        continue;
      } else if (item instanceof CredentialItem.Password) {
        continue;
      } else {
        return false;
      }
    }
    return true;
  }

  public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
    for (CredentialItem item : items) {
      if (item instanceof JgitCredentialsProvider.Passphrase) {
        ((JgitCredentialsProvider.Passphrase) item).setValue(passphrase);
      } else if (item instanceof CredentialItem.Password) {
        ((CredentialItem.Password) item).setValue(password);
      } else {
        throw new UnsupportedCredentialItem(uri, item.getPromptText());
      }
    }
    return true;
  }

}
