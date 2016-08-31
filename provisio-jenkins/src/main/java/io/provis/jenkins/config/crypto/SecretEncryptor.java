package io.provis.jenkins.config.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import com.google.common.base.Throwables;

public class SecretEncryptor {

  private SecretKey key;

  public SecretEncryptor(SecretKey key) {
    this.key = key;
  }

  public String encrypt(String value) {
    return encrypt(value, true);
  }
  
  public String encrypt(String value, boolean magic) {
    try {
      byte[] enc = encrypt(value.getBytes("UTF-8"), magic);
      return Base64.getEncoder().encodeToString(enc);
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private byte[] encrypt(byte[] value, boolean magic) {
    try {
      Cipher cipher = Cipher.getInstance(key.getAlgorithm());
      cipher.init(Cipher.ENCRYPT_MODE, key);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try(CipherOutputStream cos = new CipherOutputStream(bout, cipher)) {
        cos.write(value);
        if (magic) {
          cos.write(SecretEncryptorFactory.MAGIC);
        }
      } catch(IOException e) {
        throw Throwables.propagate(e);
      }
      return bout.toByteArray();
    } catch (GeneralSecurityException e) {
      throw new AssertionError(e);
    }
  }

}
