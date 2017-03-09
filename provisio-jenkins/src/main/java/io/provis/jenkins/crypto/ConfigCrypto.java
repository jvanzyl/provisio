package io.provis.jenkins.crypto;

import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class ConfigCrypto {

  private static final String ALGO = "AES";

  private Key pk;

  public ConfigCrypto(String encryptionKey) {
    pk = convertKey(encryptionKey);
  }

  public String decrypt(String key, String value) {
    // Base64.decode() ignores chars that are outside of base64 alphabet, incl. {{}}, so leave them here
    // value = value.substring(2, value.length() - 2);
    if (pk == null) {
      throw new IllegalStateException("Encrypted value for " + key + " encountered, but no key provided");
    }
    try {
      return new String(doCipher(Base64.decodeBase64(value), Cipher.DECRYPT_MODE), Charset.forName("UTF-8"));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to decrypt value for " + key, e);
    }
  }

  public String encrypt(String value) {
    try {
      return "{{" + Base64.encodeBase64String(doCipher(value.getBytes(Charset.forName("UTF-8")), Cipher.ENCRYPT_MODE)) + "}}";
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to encrypt value", e);
    }
  }

  public byte[] doCipher(byte[] data, int opmode) throws GeneralSecurityException {
    Cipher c = Cipher.getInstance(ALGO);
    c.init(opmode, pk);
    return c.doFinal(data);
  }

  private static Key convertKey(String keyValue) {
    if (keyValue == null) {
      return null;
    }
    byte[] keyBytes = Base64.decodeBase64(keyValue);
    return new SecretKeySpec(keyBytes, ALGO);
  }

  public static String generateEncryptionKey(int length) {
    try {
      KeyGenerator keyGen = KeyGenerator.getInstance(ALGO);
      keyGen.init(length); // for example
      SecretKey secretKey = keyGen.generateKey();
      return Base64.encodeBase64String(secretKey.getEncoded());
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to generate key", e);
    }
  }

}
