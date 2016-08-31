package io.provis.jenkins.config;

import java.io.File;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.codec.binary.Hex;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;

import io.provis.jenkins.config.ConfigTestHelper;
import io.provis.jenkins.config.MasterConfiguration;
import io.provis.jenkins.config.MasterConfiguration.MasterConfigurationBuilder;

public abstract class AbstractConfigTest extends InjectedTest {

  @Inject
  @Named("${basedir}/target/config")
  private File baseDirectory;

  protected ConfigTestHelper writeConfig(String id) throws Exception {
    return writeConfig(id, null);
  }
  
  protected ConfigTestHelper writeConfig(String id, Consumer<MasterConfigurationBuilder> hook) throws Exception {
    
    File testDir = new File(baseDirectory, id);
    
    FileUtils.deleteDirectory(testDir);
    FileUtils.forceMkdir(testDir);
    
    Properties props = new Properties();
    try (InputStream in = this.getClass().getResourceAsStream(id + ".properties")) {
      props.load(in);
    }
    
    String masterKey = Hex.encodeHexString(randomBytes(32));
    
    MasterConfigurationBuilder b = MasterConfiguration.builder()
      .masterKey(masterKey)
      .properties(props);
    
    if(hook != null) {
      hook.accept(b);
    }
    
    b.build().write(testDir);
    
    return new ConfigTestHelper(testDir, masterKey);
  }
  
  private static byte[] randomBytes(int size) {
    byte[] random = new byte[size];
    new SecureRandom().nextBytes(random);
    return random;
  }
}
