package io.provis.jenkins.config.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.google.common.io.Files;

public class SecretEncryptorFactory {

  private static final String ALGORITHM = "AES";
  private static final int DEFAULT_KEY_SIZE = 128; // default jdk export policy limitation which jenkins adheres to
  private static final SecureRandom sr = new SecureRandom();

  private static final byte[] MAGIC = "::::MAGIC::::".getBytes();

  private SecretKey masterKey;
  private File rootDir;
  private int keyBytes;

  public SecretEncryptorFactory(File rootDir) throws IOException {
    this(rootDir, null);
  }

  public SecretEncryptorFactory(File rootDir, String masterKey) throws IOException {
    this(rootDir, masterKey, DEFAULT_KEY_SIZE);
  }

  public SecretEncryptorFactory(File rootDir, String masterKey, int keySize) throws IOException {
    this.rootDir = rootDir;
    this.keyBytes = keySize / 8;

    rootDir.mkdirs();

    if (masterKey == null) {
      masterKey = Hex.encodeHexString(randomBytes(256));
    }
    
    Files.write(masterKey, new File(rootDir, "master.key"), Charset.forName("UTF-8"));
    this.masterKey = createHashedKey(masterKey.getBytes("UTF-8"));
  }

  public SecretEncryptor newEncryptor(String keyId, boolean magic) throws IOException {
    byte[] key = randomBytes(256);
    try {
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, masterKey);

      try (
        FileOutputStream fos = new FileOutputStream(new File(rootDir, keyId));
        CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {
        cos.write(key);
        cos.write(MAGIC);
      }

    } catch (GeneralSecurityException e) {
      throw new IOException("Failed to persist the key: " + keyId, e);
    }
    return new SecretEncryptor(createKey(key), magic ? MAGIC : null);
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
}
