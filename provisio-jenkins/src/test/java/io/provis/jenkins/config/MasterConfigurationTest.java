package io.provis.jenkins.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import com.google.common.io.Files;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Parent;
import de.pdark.decentxml.XMLParser;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.jenkins.config.credentials.JenkinsCredentials;
import io.provis.jenkins.config.git.GithubPluginConfig;
import io.provis.jenkins.config.templates.TemplateList;

public class MasterConfigurationTest extends InjectedTest {

  private static final String CRED_STRING = "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl";
  private static final String CRED_UPWD = "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl";
  private static final int KEYSIZE = 128 / 8;

  @Inject
  @Named("${basedir}/target/jenkins-config")
  private File baseDirectory;

  @Test
  public void validateConfigProvisioner() throws Exception {

    String masterKey = "0102030405060708090A0B0C0D0E0F1112131415161718191A1B1C1D1E1F";
    byte[] masterHash = hash(masterKey.getBytes("UTF-8"));

    Properties props = new Properties();
    props.put("config.mixins", "security.ad, jgit, users");
    props.put("key", "value");
    props.put("security.ad.domain", "adDomain");
    props.put("security.ad.server", "localhost,127.0.0.1");
    props.put("security.ad.bindPassword", "adPassword");
    
    // add some users
    props.put("users.user1.email", "test@test.com");
    props.put("users.user1.apiToken", "123456");
    props.put("users.otheruser.email", "test2@test.com");
    props.put("users.otheruser.apiToken", "abcdefgh");

    MasterConfiguration.builder()
      .properties(props)
      .masterKey(masterKey)
      .credentials(new JenkinsCredentials()
        .secretCredential("testCredential", "testSecret")
        .userCredential("usernamepassword", "username", "password"))
      .templates(TemplateList.of(MasterConfigurationTest.class, "testConfig.txt"))
      .config(new GithubPluginConfig()
         .webUrl("http://github.com")
         .apiUrl("https://api.github.com")
         .username("username")
         .oauthTokenId("apiTokenId")
         .oauthToken("oauthToken"))
      .build()
      .write(baseDirectory);
    
    // keys
    File masterKeyFile = assertExists("secrets/master.key");
    assertEquals(masterKey.toLowerCase(), Files.toString(masterKeyFile, Charset.forName("UTF-8")).toLowerCase());

    File secretKeyFile = assertExists("secrets/hudson.util.Secret");
    byte[] secretKeyEnc = Files.toByteArray(secretKeyFile);
    byte[] secretKey = decrypt(secretKeyEnc, masterHash);
    int l = secretKey.length;
    assertEquals("::::MAGIC::::", new String(secretKey, l - 13, 13));

    File textConfig = assertExists("testConfig.txt");
    String text = Files.toString(textConfig, Charset.defaultCharset());
    assertTrue(text.startsWith("value:"));
    String enc = text.substring(6, text.length());
    assertSecret("value", enc, secretKey);

    // credentials.xml
    File credsXml = assertExists("credentials.xml");
    Document credsDoc = XMLParser.parse(credsXml);

    Element cred = assertXmlExists(credsDoc, credPath(0, CRED_STRING, 0));
    assertXmlText("testCredential", cred, "id");
    assertXmlSecret("testSecret", cred, "secret", secretKey);
    
    cred = assertXmlExists(credsDoc, credPath(0, CRED_UPWD, 0));
    assertXmlText("usernamepassword", cred, "id");
    assertXmlText("username", cred, "username");
    assertXmlSecret("password", cred, "password", secretKey);

    cred = assertXmlExists(credsDoc, credPath(1, CRED_STRING, 0));
    assertXmlText("apiTokenId", cred, "id");
    assertXmlSecret("oauthToken", cred, "secret", secretKey);

    // config.xml
    File configXml = assertExists("config.xml");
    Document configDoc = XMLParser.parse(configXml);
    assertXmlExists(configDoc, "hudson/authorizationStrategy");

    assertXmlText("true", configDoc, "hudson/useSecurity");

    // ad
    Element realm = configDoc.getChild("hudson/securityRealm");
    assertNotNull(realm);
    assertEquals("hudson.plugins.active_directory.ActiveDirectorySecurityRealm", realm.getAttribute("class").getValue());
    assertXmlText("adDomain", realm, "domain");
    assertXmlText("localhost:3268,127.0.0.1:3268", realm, "server");
    assertXmlSecret("adPassword", realm, "bindPassword", secretKey);
  }

  private static String credPath(int domainIdx, String type, int credIdx) {
    return String.format(
      "/com.cloudbees.plugins.credentials.SystemCredentialsProvider"
        + "/domainCredentialsMap"
        + "/entry[%s]"
        + "/java.util.concurrent.CopyOnWriteArrayList"
        + "/%s[%s]",
        domainIdx, type, credIdx);
  }

  private File assertExists(String file) {
    File f = new File(baseDirectory, file);
    assertTrue(file + " doesn't exist", f.isFile());
    return f;
  }

  private static Element assertXmlExists(Parent p, String path) {
    Element elem = p.getChild(path);
    assertNotNull(path + " does not exist", elem);
    return elem;
  }

  private static void assertXmlText(String expectedValue, Parent p, String path) {
    Element elem = assertXmlExists(p, path);
    assertEquals(expectedValue, elem.getText());
  }

  private static void assertXmlSecret(String expectedValue, Parent p, String path, byte[] key) throws GeneralSecurityException {
    Element elem = assertXmlExists(p, path);
    assertSecret(expectedValue, elem.getText(), key);
  }

  private static void assertSecret(String expectedValue, String encrypted, byte[] key) throws GeneralSecurityException {
    assertEquals(expectedValue + "::::MAGIC::::", new String(decryptBase64(encrypted, key)));
  }

  private static byte[] hash(byte[] data) throws GeneralSecurityException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.reset();
    return digest.digest(data);
  }

  private static byte[] decryptBase64(String text, byte[] key) throws GeneralSecurityException {
    byte[] data = text.getBytes();
    data = Base64.getDecoder().decode(data);
    return decrypt(data, key);
  }

  private static byte[] decrypt(byte[] data, byte[] key) throws GeneralSecurityException {
    Cipher c = Cipher.getInstance("AES");
    c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, 0, KEYSIZE, "AES"));
    return c.doFinal(data, 0, data.length);
  }

}
