package jenkins.security;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jenkins.model.Jenkins;
import jenkins.security.ConfidentialKey;
import jenkins.security.CryptoConfidentialKey;

public class LegacyCryptoConfidentialStore extends ConfidentialStore {
  
  public static void init() {
    ConfidentialStore.TEST = new ThreadLocal<ConfidentialStore>() {
      @Override
      protected ConfidentialStore initialValue() {
        return new LegacyCryptoConfidentialStore();
      }
    };
  }
  
  public static void dispose() {
    if(ConfidentialStore.TEST != null) {
      ConfidentialStore.TEST.remove();
      ConfidentialStore.TEST = null;
    }
  }
  
  @Override
  protected void store(ConfidentialKey key, byte[] payload) throws IOException {
    if(key instanceof CryptoConfidentialKey) {
      return;
    }
    throw new IllegalArgumentException("Unsupported key type " + key.getClass().getName());
  }
  
  @Override
  protected byte[] load(ConfidentialKey key) throws IOException {
    
    if(key instanceof CryptoConfidentialKey) {
      MessageDigest digest;
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
      digest.reset();
      digest.update(Jenkins.getInstance().getSecretKey().getBytes("UTF-8"));
      return digest.digest();
    }
    
    throw new IllegalArgumentException("Unsupported key type " + key.getClass().getName());
  }

  @Override
  public byte[] randomBytes(int size) {
    return new byte[0];
  }
  
}
