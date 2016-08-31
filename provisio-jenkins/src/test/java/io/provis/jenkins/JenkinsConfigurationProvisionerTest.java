package io.provis.jenkins;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import io.provis.SimpleProvisioner;
import io.provis.jenkins.config.ConfigTestHelper;
import io.provis.jenkins.config.Configuration;

public class JenkinsConfigurationProvisionerTest extends InjectedTest {
  
  @Inject
  @Named("${basedir}/target/config/provisioner")
  private File baseDirectory;
  
  @Inject
  @Named("${basedir}/provisioner/repo")
  private File repoDirectory;
  
  @Test
  public void testExternalDependencies() throws Exception {
    
    JenkinsConfigurationProvisioner p = new JenkinsConfigurationProvisioner(repoDirectory, SimpleProvisioner.DEFAULT_REMOTE_REPO);
    
    Configuration c = new Configuration()
      .set("config.dependencies", "io.provis.jenkins.test:customconfig:1.0")
      .set("config.mixins", "test")
      .set("test.foo", "bar");
    
    File dir = new File(baseDirectory, "externalDeps");
    p.provision(c, null, dir);
    
    ConfigTestHelper h = new ConfigTestHelper(dir, null);
    Document doc = h.asXml("test.xml");
    h.assertXmlText("bar", doc, "test/foo");
    
  }
}
