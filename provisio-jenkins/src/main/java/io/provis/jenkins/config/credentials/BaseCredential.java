package io.provis.jenkins.config.credentials;

import java.util.UUID;

public abstract class BaseCredential {
  
  private String id;
  private String description;
  
  public BaseCredential() {
    this(null);
  }
  
  public BaseCredential(String id) {
    this(id, id);
  }
  
  public BaseCredential(String id, String description) {
    this.id = id == null ? UUID.randomUUID().toString() : id;
    this.description = description == null ? this.id : description;
  }
  
  public String getId() {
    return id;
  }
  
  public String getDescription() {
    return description;
  }
  
}
