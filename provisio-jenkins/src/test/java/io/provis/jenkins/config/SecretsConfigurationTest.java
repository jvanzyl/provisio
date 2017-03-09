package io.provis.jenkins.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.io.Files;

import io.provis.jenkins.config.templates.TemplateList;

public class SecretsConfigurationTest extends AbstractConfigTest {

  @Test
  public void testSecretKeyRetention() throws Exception {
    String encryptionKey = "fIg2cbN374RnIOuZhrW2Rw==";

    ConfigTestHelper h = writeConfig("secrets", encryptionKey,
      b -> b.templates(TemplateList.of(SecretsConfigurationTest.class, "testConfig.txt")));

    assertEquals("000102030405060708090a0b0c0d0e0f10111213", h.getMasterKeyHex());
    assertEquals("1415161718191a1b1c1d1e1f2021222324252627", h.getSecretKeyHex());

    File textConfig = h.assertExists("testConfig.txt");
    String text = Files.toString(textConfig, Charset.defaultCharset());
    assertTrue(text.startsWith("value:"));
    String enc = text.substring(6, text.length());
    assertEquals("yUaWhviE2QBs+mWBisr7asXv9kFPY2Dz5OM5oW7cIgM=", enc);
    h.assertSecret("value", enc);
  }

}
