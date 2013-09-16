package io.provis.maven;

import io.provis.maven.execute.MavenInvoker;
import io.provis.maven.execute.MavenRequest;
import io.provis.maven.execute.MavenResult;
import io.provis.maven.provision.MavenProvisioner;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

public class MavenInvokerTest extends ProvisioningTestCase {

  @Inject
  @Named("forked")
  private MavenInvoker maven;
  
  @Inject
  private MavenProvisioner provisioner;
  
  @Inject
  @Named("${basedir}/target/maven")
  private File mavenHome;
  
  @Test
  public void testMavenExecution() throws Exception {
    
    FileUtils.deleteDirectory(mavenHome.getAbsolutePath());
    
    provisioner.provision("3.0.3", mavenHome);
    
    MavenRequest request = new MavenRequest()
     .setMavenHome(mavenHome)
     .addGoals("validate")
     .setWorkDir(System.getProperty("user.dir"));
    
    MavenResult result = maven.invoke(request);    
  }
}
