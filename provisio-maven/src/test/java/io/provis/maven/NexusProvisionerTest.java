package io.provis.maven;

import io.provis.nexus.NexusForkedLauncher;
import io.provis.nexus.NexusProvisioner;
import io.provis.nexus.NexusProvisioningContext;
import io.tesla.aether.guice.maven.MavenBehaviourModule;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import com.google.inject.Binder;

public class NexusProvisionerTest extends ProvisioningTestCase {

  @Inject
  @Named("${basedir}/target/nexus")
  private File baseDirectory;
  
  @Inject
  private NexusProvisioner provisioner;
    
  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }
  
  @Test
  public void testMavenExecution() throws Exception {
    
    FileUtils.deleteDirectory(baseDirectory);
    
    NexusProvisioningContext context = new NexusProvisioningContext();
    context.setPro(true);
    context.setVersion("2.5.0-01");
    context.setInstallationDirectory(new File(baseDirectory,"installation"));
    context.setWorkDirectory(new File(baseDirectory,"work"));
    context.addUser("userA","admin123");
    context.setPort(9005);    
    provisioner.provision(context);
        
    NexusForkedLauncher launcher = new NexusForkedLauncher(context);
    launcher.start();
    launcher.stop();
  }  
}
