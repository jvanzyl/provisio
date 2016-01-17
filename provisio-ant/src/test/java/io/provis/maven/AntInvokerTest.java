package io.provis.maven;

import static org.junit.Assert.fail;
import io.provis.ant.AntInvoker;
import io.provis.ant.AntProvisioner;
import io.provis.ant.AntRequest;
import io.provis.ant.AntResult;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

public class AntInvokerTest extends InjectedTest {

  @Inject
  @Named("forked")
  private AntInvoker ant;

  @Inject
  private AntProvisioner provisioner;

  @Inject
  @Named("${basedir}/target/ant")
  private File antHome;

  @Inject
  @Named("${basedir}/src/test/projects/ant/simple")
  private File projectDirectory;

  @Test
  public void testAntExecution() throws Exception {

    FileUtils.deleteDirectory(antHome.getAbsolutePath());

    provisioner.provision("1.7.1", antHome);

    AntRequest request = new AntRequest()
      .setAntHome(antHome)
      .addTargets("simple")
      .setWorkDir(projectDirectory);

    AntResult result = ant.invoke(request);
    if (result.getErrors().isEmpty() == false) {
      for (Throwable error : result.getErrors()) {
        error.printStackTrace();
      }
      fail();
    }
  }
}
