package io.provis.jenkins;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;
import io.provis.jenkins.config.JenkinsConfigRuntimeProvisioner;
import io.provis.jenkins.config.MasterConfiguration;

import static org.junit.Assert.*;

public class JenkinsConfigRuntimeProvisionerTest extends InjectedTest {
  @Inject
  @Named("${basedir}/target/jenkins-config")
  private File baseDirectory;

  @Inject
  private JenkinsConfigRuntimeProvisioner provisioner;

  @Test
  public void validateConfigProvisioner() throws Exception {
    
    String key = "0102030405060708090A0B0C0D0E0F1112131415161718191A1B1C1D1E1F";
    
    byte[] secretKey = Hex.decodeHex(key.toCharArray());
    
    try(MasterConfiguration mc = MasterConfiguration.builder()
      .provisioner(provisioner)
      .outputDirectory(baseDirectory)
      .secretKey(secretKey)
      .secretCredential("testCredential", "testSecret")
      .build()) {
      mc.write();
    }
    
    File secretFile = assertExists("secret.key");
    assertEquals(key.toLowerCase(), FileUtils.fileRead(secretFile).toLowerCase());
    
    assertExists("secret.key.not-so-secret");
    assertExists("jenkins.security.RekeySecretAdminMonitor/scanOnBoot");
    
    File credsXml = assertExists("credentials.xml");
    Document doc = XMLParser.parse(credsXml);
    Element elem = doc.getChild(
        "/com.cloudbees.plugins.credentials.SystemCredentialsProvider"
            + "/domainCredentialsMap"
              + "/entry"
                + "/java.util.concurrent.CopyOnWriteArrayList"
                  + "/org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl"
                    + "/secret");
    
    assertNotNull(elem);
    assertEquals("XPCos9cftJPN29xfrvZtz0zCflnHzcU4DmAadATYw3M=", elem.getText());
  }
  
  private File assertExists(String file) {
    File f = new File(baseDirectory, file);
    assertTrue(file + " doesn't exist", f.isFile());
    return f;
  }
}
