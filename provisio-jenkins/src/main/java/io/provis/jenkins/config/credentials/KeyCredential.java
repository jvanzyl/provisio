package io.provis.jenkins.config.credentials;

public class KeyCredential extends BaseCredential {

  private final String username;
  private final String passphrase;
  private final KeySource source;

  public KeyCredential(String id, String description, String username, String passphrase, KeySource source) {
    super(id, description);
    this.username = username;
    this.passphrase = passphrase;
    this.source = source;
  }

  public String getUsername() {
    return username;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public KeySource getSource() {
    return source;
  }

  public static interface KeySource {
  }

  public static class DirectKeySource implements KeySource {
    private final String privateKey;

    public DirectKeySource(String privateKey) {
      this.privateKey = privateKey;
    }

    public String getPrivateKey() {
      return privateKey;
    }
  }

  public static class FileOnMasterSource implements KeySource {
    private final String keyFileOnMaster;

    public FileOnMasterSource(String keyFileOnMaster) {
      this.keyFileOnMaster = keyFileOnMaster;
    }

    public String getKeyFileOnMaster() {
      return keyFileOnMaster;
    }
  }

  public static class UsersKeySource implements KeySource {
    public boolean isUsersKey() {
      return true;
    }
  }
}
