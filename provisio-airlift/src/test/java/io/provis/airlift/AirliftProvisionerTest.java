package io.provis.airlift;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import io.provis.airlift.AirliftLauncher;
import io.provis.airlift.AirliftProvisioner;
import io.provis.airlift.AirliftProvisioningContext;

public class AirliftProvisionerTest extends InjectedTest {

  @Inject
  @Named("${basedir}/target/airlift")
  private File baseDirectory;

  @Inject
  private AirliftProvisioner provisioner;

  @Test
  public void validateAirliftServerProvisioner() throws Exception {
    FileUtils.deleteDirectory(baseDirectory);
    AirliftProvisioningContext context = new AirliftProvisioningContext();
    context.setPort(8000);
    context.setServerHome(new File(baseDirectory, "server"));
    context.setServerCoordinate("io.provis:provisio-airlift-testserver:tar.gz:0.1.29-SNAPSHOT");
    context.setStatusUrl(String.format("http://localhost:%s/api/status", context.getPort()));
    provisioner.provision(context);
    AirliftLauncher launcher = new AirliftLauncher(context);
    try {
      launcher.start();
    } finally {
      launcher.stop();
    }
  }
}
