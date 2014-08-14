package io.provis.maven;

import io.provis.nexus.NexusForkedLauncher;
import io.provis.nexus.NexusProvisioner;
import io.provis.nexus.NexusProvisioningContext;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class NexusProvisionerTest extends InjectedTest {

  @Inject
  @Named("${basedir}/target/nexus")
  private File baseDirectory;
  
  @Inject
  private NexusProvisioner provisioner;
      
  @Test
  public void testMavenExecution() throws Exception {
    
    FileUtils.deleteDirectory(baseDirectory);
    
    NexusProvisioningContext context = new NexusProvisioningContext();
    context.setVersion("2.6.0");
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
