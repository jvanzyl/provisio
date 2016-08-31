package io.provis.jenkins.config.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.google.common.io.Files;

public class SecretEncryptorFactory {

  private static final String ALGORITHM = "AES";
  private static final int DEFAULT_KEY_SIZE = 128; // default jdk export policy limitation which jenkins adheres to
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final SecureRandom sr = new SecureRandom();

  static final byte[] MAGIC = "::::MAGIC::::".getBytes();

  private byte[] masterKey;
  private int keyBytes;
  
  private Map<String, Encryptor> encryptors = new HashMap<>();

  public SecretEncryptorFactory() {
    this( null);
  }

  public SecretEncryptorFactory(String masterKey) {
    this(masterKey, DEFAULT_KEY_SIZE);
  }

  public SecretEncryptorFactory(String masterKey, int keySize) {
    this.keyBytes = keySize / 8;
    if (masterKey == null) {
      masterKey = Hex.encodeHexString(randomBytes(256));
    }
    this.masterKey = masterKey.getBytes(UTF8);
  }

  public SecretEncryptor encryptor(String keyId) {
    
    Encryptor encryptor = encryptors.get(keyId);
    if(encryptor == null) {
      byte[] key = randomBytes(256);
      encryptor = new Encryptor(keyId, key, new SecretEncryptor(createKey(key)));
      encryptors.put(keyId, encryptor);
    }
    return encryptor.encryptor;
  }

  private SecretKey createKey(byte[] key) {
    return new SecretKeySpec(key, 0, keyBytes, ALGORITHM);
  }

  private SecretKey createHashedKey(byte[] key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.reset();
      digest.update(key);
      return new SecretKeySpec(digest.digest(), 0, keyBytes, ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }

  private static byte[] randomBytes(int size) {
    byte[] random = new byte[size];
    sr.nextBytes(random);
    return random;
  }
  
  public void write(File rootDir) throws IOException {
    Files.write(masterKey, new File(rootDir, "master.key"));
    
    SecretKey master = createHashedKey(masterKey);
    
    for(Encryptor enc: encryptors.values()) {
      try {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, master);
        try (
          FileOutputStream fos = new FileOutputStream(new File(rootDir, enc.keyId));
          CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
          cos.write(enc.keyBytes);
          cos.write(MAGIC);
        }
      } catch (GeneralSecurityException e) {
        throw new IOException("Failed to persist the key: " + enc.keyId, e);
      }
    }
    
  }
  
  private static class Encryptor {
    final String keyId;
    final byte[] keyBytes;
    final SecretEncryptor encryptor;
    
    public Encryptor(String keyId, byte[] keyBytes, SecretEncryptor encryptor) {
      this.keyId = keyId;
      this.keyBytes = keyBytes;
      this.encryptor = encryptor;
    }
  }
}
