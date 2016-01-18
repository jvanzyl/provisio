package io.provis.nexus;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class JenkinsProvisionerTest extends InjectedTest {

  @Inject
  @Named("${basedir}/target/jenkins")
  private File baseDirectory;

  @Inject
  private JenkinsProvisioner provisioner;

  @Test
  public void validateJenkinsProvisioner() throws Exception {
    FileUtils.deleteDirectory(baseDirectory);
    JenkinsProvisioningContext context = new JenkinsProvisioningContext();
    context.setVersion("1.644");
    context.setInstallationDirectory(new File(baseDirectory, "installation"));
    context.setWorkDirectory(new File(baseDirectory, "work"));
    context.addUser("userA", "admin123");
    context.addPlugin("org.jenkins-ci.plugins:scm-api:hpi:1.0");
    context.addPlugin("org.jenkins-ci.plugins:git-client:hpi:1.19.0");
    context.addPlugin("org.jenkins-ci.plugins:git:hpi:2.4.0");
    context.setPort(9006);
    provisioner.provision(context);
    // Launch Jenkins with the provisioning context
    JenkinsLauncher launcher = new JenkinsLauncher(context);
    launcher.start();
    launcher.stop();
  }
}
