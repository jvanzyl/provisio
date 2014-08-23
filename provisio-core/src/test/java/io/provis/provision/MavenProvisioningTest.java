package io.provis.provision;

import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.takari.aether.connector.AetherRepositoryConnectorFactory;

import java.io.File;
import java.io.FileInputStream;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.junit.Before;

import com.google.common.collect.ImmutableList;

public abstract class MavenProvisioningTest {

  protected MavenProvisioner provisioner;
  protected String basedir;

  private RuntimeReader reader;
  
  protected abstract ProvionsingConfig provisioningConfig() throws Exception;
  
  @Before
  public void serviceLocator() throws Exception {

    reader = new RuntimeReader(Actions.defaultActionDescriptors());
    
    ProvionsingConfig provisioningConfig = provisioningConfig();

    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(FileProcessor.class, DefaultFileProcessor.class);

    // RepositorySystem we can use for testing that has Maven's behaviour
    RepositorySystem system = locator.getService(RepositorySystem.class);

    // RepositorySystemSesssion we can use for testing that has Maven's behaviour
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(provisioningConfig.getLocalRepository());
    session.setTransferListener(new ConsoleTransferListener());
    session.setRepositoryListener(new ConsoleRepositoryListener());

    // If we're working against our local repository manager we want to force
    DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
    mirrorSelector.add("central", provisioningConfig.getRemoteRepositoryUrl(), "default", false, "external:*", "default");
    session.setMirrorSelector(mirrorSelector);

    // This prevents any of the _remote.repositories files from being written, we don't need them in a remote repository
    SimpleLocalRepositoryManagerFactory f = new SimpleLocalRepositoryManagerFactory();
    session.setLocalRepositoryManager(f.newInstance(session, localRepo));

    RemoteRepository remoteRepository = new RemoteRepository.Builder("central", "default", provisioningConfig.getRemoteRepositoryUrl()).build();
    provisioner = new DefaultMavenProvisioner(system, session, ImmutableList.of(remoteRepository));
  }

  //
  // Helpers
  //
  
  protected Runtime runtime(String runtimeName) throws Exception {
    return reader.read(new FileInputStream(new File(basedir, String.format("src/test/runtimes/%s/assembly.xml",runtimeName))));
  }
  
  protected String getRemoteRepositoryUrl(String runtimeName) throws Exception {
    return new File(basedir, String.format("src/test/runtimes/%s/repo",runtimeName)).toURI().toURL().toExternalForm();
  }

  protected final String getBasedir() {
    if (null == basedir) {
      basedir = System.getProperty("basedir", new File("").getAbsolutePath());
    }
    return basedir;
  }

}
