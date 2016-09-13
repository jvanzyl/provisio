package io.provis.jenkins.aether;

public class Repository {
  
  private String id;
  private String url;
  private String username;
  private String password;
  
  public Repository(String id, String url) {
    this.id = id;
    this.url = url;
  }
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }
}
