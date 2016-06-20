package io.provis.jenkins.runtime;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

public interface JenkinsRuntime extends Closeable {

  void writeCredentials(CredentialContainer creds) throws IOException;

  String encrypt(String value);
  
  public static class CredentialContainer {
    private List<SecretCredential> secretCredentials;
    private List<UsernamePassword> usernamePasswordCredentials;
    private List<GitHubCredential> gitHubCredentials;
    
    public CredentialContainer(List<SecretCredential> secretCredentials, List<UsernamePassword> usernamePasswordCredentials, List<GitHubCredential> gitHubCredentials) {
      this.secretCredentials = secretCredentials;
      this.usernamePasswordCredentials = usernamePasswordCredentials;
      this.gitHubCredentials = gitHubCredentials;
    }
    
    public List<SecretCredential> getSecretCredentials() {
      return secretCredentials;
    }
    
    public List<UsernamePassword> getUsernamePasswordCredentials() {
      return usernamePasswordCredentials;
    }
    
    public List<GitHubCredential> getGitHubCredentials() {
      return gitHubCredentials;
    }
  }
  
  
  public static class UsernamePassword {
    private final String id;
    private final String username;
    private final String password;

    public UsernamePassword(String id, String username, String password) {
      this.id = id;
      this.username = username;
      this.password = password;
    }
    
    public String getId() {
      return id;
    }
    public String getUsername() {
      return username;
    }
    public String getPassword() {
      return password;
    }
  }
  
  public static class SecretCredential {
    private final String id;
    private final String secret;

    public SecretCredential(String id, String secret) {
      this.id = id;
      this.secret = secret;
    }
    
    public String getId() {
      return id;
    }
    public String getSecret() {
      return secret;
    }
  }

  public static class GitHubCredential {
    private final String id;
    private final String username;
    private final String oauthToken;
    private final String gitHubApiUrl;

    public GitHubCredential(String id, String username, String oauthToken, String gitHubApiUrl) {
      this.id = id;
      this.username = username;
      this.oauthToken = oauthToken;
      this.gitHubApiUrl = gitHubApiUrl;
    }
    
    public String getId() {
      return id;
    }
    public String getUsername() {
      return username;
    }
    public String getOauthToken() {
      return oauthToken;
    }
    public String getGitHubApiUrl() {
      return gitHubApiUrl;
    }
  }
  
}
