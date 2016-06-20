package io.provis.jenkins;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.*;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;

public class JenkinsProvisionerTest extends InjectedTest {

  @Inject
  @Named("${basedir}/target/jenkins")
  private File baseDirectory;

  @Inject
  private JenkinsProvisioner provisioner;
  
  @Test
  public void coordinateToPathTest() {
    
    assertEquals("org/jenkins-ci/plugins/scm-api/1.0/scm-api-1.0.hpi", provisioner.coordinateToPath("org.jenkins-ci.plugins:scm-api:hpi:1.0"));
    
    assertEquals("org/jenkins-ci/plugins/scm-api/1.0/scm-api-1.0.jar", provisioner.coordinateToPath("org.jenkins-ci.plugins:scm-api:1.0"));
 
    assertEquals("com/coravy/hudson/plugins/github/github/1.17.1/github-1.17.1.hpi", provisioner.coordinateToPath("com.coravy.hudson.plugins.github:github:hpi:1.17.1"));

  }
  
//  @Ignore
  @Test
  public void jenkinsFileNameTest() {
    JenkinsProvisioningContext context = new JenkinsProvisioningContext();
    assertEquals("jenkins-war.war", context.getJenkinsFileName());
    
    context.setVersion("1.644");
    assertEquals("jenkins-war-1.644.war", context.getJenkinsFileName());
  }

//  @Ignore
  @Test
  public void validateJenkinsProvisionerAdd() throws Exception {
    FileUtils.deleteDirectory(baseDirectory);
    
    List<String> jpis = Arrays.asList("scm-api", "git-client", "git");
    
    for (String jpi : jpis) {
      assertFalse(Paths.get(baseDirectory.getAbsolutePath(), "work", "plugins", jpi + ".jpi").toFile().exists());
    }
    
    JenkinsProvisioningContext context = new JenkinsProvisioningContext();
    context.setVersion("1.644");
    context.setInstallationDirectory(new File(baseDirectory, "installation"));
    context.setWorkDirectory(new File(baseDirectory, "work"));
    // Add Git capabilities to the Jenkins server
    context.addPlugin("org.jenkins-ci.plugins:scm-api:hpi:1.0");
    context.addPlugin("org.jenkins-ci.plugins:git-client:hpi:1.19.0");
    context.addPlugin("org.jenkins-ci.plugins:git:hpi:2.4.0");
    context.setPort(9006);
    provisioner.provision(context);
    // Launch Jenkins with the provisioning context
    JenkinsLauncher launcher = new JenkinsLauncher(context);
    launcher.start();
    launcher.stop();
    
    for (String jpi : jpis) {
      assertTrue(Paths.get(baseDirectory.getAbsolutePath(), "work", "plugins", jpi + ".jpi").toFile().exists());
    }
    
  }
  
//  @Ignore
  @Test
  public void validateJenkinsProvisionerDist() throws Exception {
    
    String _fakeWar = "/war/fake.war";
    
    File fakeWar = Paths.get(JenkinsProvisionerTest.class.getResource(_fakeWar).toURI()).toFile();
    JenkinsProvisioningContext context = new JenkinsProvisioningContext();
    context.setVersion("1.625.3");
    context.setDist(fakeWar);
    context.setInstallationDirectory(new File(baseDirectory, "installation"));
    context.setWorkDirectory(new File(baseDirectory, "work"));
    context.setPort(9006);
    provisioner.provision(context);
    
    File expectedDist = Paths.get(baseDirectory.getAbsolutePath(), "installation", context.getJenkinsFileName()).toFile();
    
    assertTrue(expectedDist.exists());

    HashCode hash1 = Files.asByteSource(fakeWar).hash(Hashing.sha1());
    HashCode hash2 = Files.asByteSource(expectedDist).hash(Hashing.sha1());
    
    assertEquals(hash1, hash2);
    
  }
  
//  @Ignore
  @Test
  public void validateJenkinsProvisionerGithub() throws Exception {

    File expected = Paths.get(baseDirectory.getAbsolutePath(), "work", "plugins", "github.jpi").toFile();
    assertFalse(expected.exists());

    JenkinsProvisioningContext context = new JenkinsProvisioningContext();
    context.setVersion("1.625.3");
    context.setInstallationDirectory(new File(baseDirectory, "installation"));
    context.setWorkDirectory(new File(baseDirectory, "work"));
    context.addPlugin("org.jenkins-ci.plugins:github-api:hpi:1.75");
    context.addPlugin("org.jenkins-ci.plugins:token-macro:hpi:1.11");
    context.addPlugin("org.jenkins-ci.plugins:plain-credentials:1.1");
    context.addPlugin("com.coravy.hudson.plugins.github:github:hpi:1.17.1");
    context.setPort(9006);
    provisioner.provision(context);
    
    assertTrue(expected.exists());
    
  }

}
