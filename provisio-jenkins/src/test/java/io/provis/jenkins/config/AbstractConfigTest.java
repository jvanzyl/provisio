package io.provis.jenkins.config;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Named;

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
    return writeConfig(id, null, hook);
  }

  protected ConfigTestHelper writeConfig(String id, String configEncryptionKey, Consumer<MasterConfigurationBuilder> hook) throws Exception {

    File testDir = new File(baseDirectory, id);

    FileUtils.deleteDirectory(testDir);
    FileUtils.forceMkdir(testDir);

    Configuration config = new Configuration();
    try (InputStream in = this.getClass().getResourceAsStream(id + ".properties")) {
      config.load(in);
    }

    config.decryptValues(configEncryptionKey);

    MasterConfigurationBuilder b = MasterConfiguration.builder()
      .configuration(config);

    if (hook != null) {
      hook.accept(b);
    }

    b.build().write(testDir, true);

    return new ConfigTestHelper(testDir, b.encryption().getMasterKeyHex());
  }

}
