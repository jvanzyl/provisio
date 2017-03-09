package io.provis.jenkins.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;

import com.google.common.io.Files;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Parent;
import de.pdark.decentxml.XMLParser;

public class ConfigTestHelper {

  private static final int KEYSIZE = 128 / 8;

  private File baseDirectory;

  private String masterKey;
  private byte[] secretKey;


  public ConfigTestHelper(File baseDirectory, String masterKey) throws Exception {
    this.baseDirectory = baseDirectory;
    this.masterKey = masterKey;
  }

  private byte[] getSecretKey() throws Exception {
    if (secretKey == null) {
      if (masterKey == null) {
        throw new IllegalStateException("No master key provided");
      }

      byte[] masterHash = hash(masterKey.getBytes("UTF-8"));
      File secretKeyFile = assertExists("secrets/hudson.util.Secret");
      byte[] secretKeyEnc = Files.toByteArray(secretKeyFile);
      byte[] key = decrypt(secretKeyEnc, masterHash);

      // test keys
      File masterKeyFile = assertExists("secrets/master.key");
      assertEquals(masterKey.toLowerCase(), Files.toString(masterKeyFile, Charset.forName("UTF-8")).toLowerCase());

      int l = key.length;
      assertEquals("::::MAGIC::::", new String(key, l - 13, 13));

      secretKey = key;
    }
    return secretKey;
  }

  public String getSecretKeyHex() throws Exception {
    byte[] secretKey = getSecretKey();
    byte[] actualKey = new byte[secretKey.length - 13];
    System.arraycopy(secretKey, 0, actualKey, 0, actualKey.length);
    return Hex.encodeHexString(actualKey);
  }

  public String getMasterKeyHex() {
    return masterKey;
  }

  public File assertExists(String file) {
    File f = new File(baseDirectory, file);
    assertTrue(file + " doesn't exist", f.isFile());
    return f;
  }

  public Document asXml(String file) throws IOException {
    return XMLParser.parse(assertExists(file));
  }

  public Element assertXmlElement(Parent p, String path) {
    Element elem = p.getChild(path);
    assertNotNull(path + " does not exist", elem);
    return elem;
  }

  public void assertXmlAttribute(String expectedValue, Element e, String name) {
    assertEquals(expectedValue, e.getAttributeValue(name));
  }

  public void assertXmlText(String expectedValue, Parent p, String path) {
    Element elem = assertXmlElement(p, path);
    assertEquals(expectedValue, elem.getText());
  }

  public void assertXmlSecret(String expectedValue, Parent p, String path) throws Exception {
    Element elem = assertXmlElement(p, path);
    assertSecret(expectedValue, elem.getText());
  }

  public void assertSecret(String expectedValue, String encrypted) throws Exception {
    assertEquals(expectedValue + "::::MAGIC::::", new String(decryptBase64(encrypted, getSecretKey())));
  }

  public byte[] hash(byte[] data) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.reset();
    return digest.digest(data);
  }

  public byte[] decryptBase64(String text, byte[] key) throws Exception {
    byte[] data = text.getBytes();
    data = Base64.getDecoder().decode(data);
    return decrypt(data, key);
  }

  public byte[] decrypt(byte[] data, byte[] key) throws Exception {
    Cipher c = Cipher.getInstance("AES");
    c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, 0, KEYSIZE, "AES"));
    return c.doFinal(data, 0, data.length);
  }
}
