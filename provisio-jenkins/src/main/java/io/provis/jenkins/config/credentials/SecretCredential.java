package io.provis.jenkins.config.credentials;

public class SecretCredential extends BaseCredential {
  private final String secret;

  public SecretCredential(String id, String description, String secret) {
    super(id, description);
    this.secret = secret;
  }
  
  public String getSecret() {
    return secret;
  }
}