package io.provis.jenkins.config.credentials;

public class UsernamePassword extends BaseCredential {
  private final String username;
  private final String password;

  public UsernamePassword(String id, String description, String username, String password) {
    super(id, description);
    this.username = username;
    this.password = password;
  }
  
  public String getUsername() {
    return username;
  }
  public String getPassword() {
    return password;
  }
}