package io.provis.airlift;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

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

  @Test
  public void validateAirliftServerProvisioner() throws Exception {
    
    Properties config = new Properties();
    try(InputStream in = getClass().getResourceAsStream("/config.properties")) {
      config.load(in);
    }
    String localRepository = config.getProperty("localRepository");
    String version = config.getProperty("version");
    
    FileUtils.deleteDirectory(baseDirectory);
    AirliftProvisioningContext context = new AirliftProvisioningContext();
    context.setPort(8000);
    context.setServerHome(new File(baseDirectory, "server"));
    context.setServerCoordinate("io.provis:provisio-airlift-testserver:tar.gz:" + version);
    context.setStatusUrl(String.format("http://localhost:%s/api/status", context.getPort()));
    
    AirliftProvisioner provisioner = new AirliftProvisioner(new File(localRepository), AirliftProvisioner.DEFAULT_REMOTE_REPO);
    provisioner.provision(context);
    AirliftLauncher launcher = new AirliftLauncher(context);
    try {
      launcher.start();
    } finally {
      launcher.stop();
    }
  }
}
