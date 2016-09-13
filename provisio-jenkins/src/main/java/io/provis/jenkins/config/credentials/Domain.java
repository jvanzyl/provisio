package io.provis.jenkins.config.credentials;

import java.util.ArrayList;
import java.util.List;

public class Domain {
  private String name;
  private String description;
  private String scheme;
  private String host;
  private List<SecretCredential> secretCredentials = new ArrayList<>();
  private List<UsernamePassword> usernamePasswordCredentials = new ArrayList<>();
  private List<KeyCredential> keyCredentials = new ArrayList<>();

  public Domain(String name, String description, String scheme, String host) {
    this.name = name;
    this.description = description;
    this.scheme = scheme;
    this.host = host;
  }

  public boolean isGlobal() {
    return name == null;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getScheme() {
    return scheme;
  }

  public String getHost() {
    return host;
  }

  public List<SecretCredential> getSecretCredentials() {
    return secretCredentials;
  }

  public List<UsernamePassword> getUsernamePasswordCredentials() {
    return usernamePasswordCredentials;
  }
  
  public List<KeyCredential> getKeyCredentials() {
    return keyCredentials;
  }
}