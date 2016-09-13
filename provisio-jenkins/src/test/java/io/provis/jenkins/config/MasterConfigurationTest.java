package io.provis.jenkins.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.io.Files;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import io.provis.jenkins.config.credentials.JenkinsCredentials;
import io.provis.jenkins.config.git.GithubPluginConfig;
import io.provis.jenkins.config.templates.TemplateList;

public class MasterConfigurationTest extends AbstractConfigTest {

  private static final String CRED_STRING = "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl";
  private static final String CRED_UPWD = "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl";

  @Test
  public void testFull() throws Exception {
    
    ConfigTestHelper h = writeConfig("full", 
      b -> b
        .credentials(new JenkinsCredentials()
          .secretCredential("testCredential", "testSecret")
          .userCredential("usernamepassword", "username", "password"))
        .config(new GithubPluginConfig()
          .webUrl("http://github.com")
          .apiUrl("https://api.github.com")
          .username("username")
          .oauthTokenId("apiTokenId")
          .oauthToken("oauthToken"))
        .templates(TemplateList.of(MasterConfigurationTest.class, "testConfig.txt")));
    
    File textConfig = h.assertExists("testConfig.txt");
    String text = Files.toString(textConfig, Charset.defaultCharset());
    assertTrue(text.startsWith("value:"));
    String enc = text.substring(6, text.length());
    h.assertSecret("value", enc);

    // credentials.xml
    Document creds = h.asXml("credentials.xml");
    
    Element cred;
    
    cred = h.assertXmlElement(creds, credPath(0, CRED_UPWD, 0));
    h.assertXmlText("cred1", cred, "id");
    h.assertXmlText("someguy", cred, "username");
    h.assertXmlSecret("abcdefgh", cred, "password");

    cred = h.assertXmlElement(creds, credPath(0, CRED_STRING, 0));
    h.assertXmlText("testCredential", cred, "id");
    h.assertXmlSecret("testSecret", cred, "secret");
    
    cred = h.assertXmlElement(creds, credPath(0, CRED_UPWD, 1));
    h.assertXmlText("usernamepassword", cred, "id");
    h.assertXmlText("username", cred, "username");
    h.assertXmlSecret("password", cred, "password");

    cred = h.assertXmlElement(creds, credPath(1, CRED_UPWD, 0));
    h.assertXmlText("cred2", cred, "id");
    h.assertXmlText("Some other guy's credential", cred, "description");
    h.assertXmlText("someotherguy", cred, "username");
    h.assertXmlSecret("qwerty", cred, "password");

    cred = h.assertXmlElement(creds, credPath(1, CRED_STRING, 0));
    h.assertXmlText("cred3", cred, "id");
    h.assertXmlSecret("12345", cred, "secret");

    cred = h.assertXmlElement(creds, credPath(2, CRED_STRING, 0));
    h.assertXmlText("apiTokenId", cred, "id");
    h.assertXmlSecret("oauthToken", cred, "secret");

    // config.xml
    Document config = h.asXml("config.xml");
    h.assertXmlText("true", config, "hudson/useSecurity");
    h.assertXmlElement(config, "hudson/authorizationStrategy");

    // ad
    Element realm = h.assertXmlElement(config, "hudson/securityRealm");
    assertEquals("hudson.plugins.active_directory.ActiveDirectorySecurityRealm", realm.getAttribute("class").getValue());
    h.assertXmlText("adDomain", realm, "domain");
    h.assertXmlText("localhost:3268,127.0.0.1:3268", realm, "server");
    h.assertXmlSecret("adPassword", realm, "bindPassword");
  }
  
  @Test
  public void testGithubAuth() throws Exception {
    ConfigTestHelper h = writeConfig("githubauth");
    
    Document config = h.asXml("config.xml");
    h.assertXmlText("true", config, "hudson/useSecurity");
    Element realm = config.getChild("hudson/securityRealm");
    
    h.assertXmlAttribute("org.jenkinsci.plugins.GithubSecurityRealm", realm, "class");
    
    h.assertXmlText("webUrl", realm, "githubWebUri");
    h.assertXmlText("apiUrl", realm, "githubApiUri");
    h.assertXmlText("clientId", realm, "clientID");
    h.assertXmlSecret("clientSecret", realm, "clientSecret");
    h.assertXmlText("oauthScopes", realm, "oauthScopes");
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

}
