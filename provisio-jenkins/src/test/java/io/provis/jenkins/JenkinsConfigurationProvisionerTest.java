package io.provis.jenkins;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.model.Model;
import org.apache.maven.repository.internal.ArtifactDescriptorReaderDelegate;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Test;

import de.pdark.decentxml.Document;
import io.provis.MavenProvisioner;
import io.provis.SimpleProvisioner;
import io.provis.jenkins.aether.ResolutionSystem;
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

    ResolutionSystem resolutionSystem = new ResolutionSystem(repoDirectory);
    resolutionSystem.remoteRepository(SimpleProvisioner.DEFAULT_REMOTE_REPO);
    MavenProvisioner provisioner = new MavenProvisioner(resolutionSystem.repositorySystem(), newRepositorySystemSession(repoDirectory), resolutionSystem.remoteRepositories());

    JenkinsConfigurationProvisioner p = new JenkinsConfigurationProvisioner(provisioner);

    Configuration c = new Configuration()
        .set("config.dependencies", "io.provis.jenkins.test:customconfig:1.0")
        .set("test.foo", "bar");

    File dir = new File(baseDirectory, "externalDeps");
    p.provision(c, null, dir, true);

    ConfigTestHelper h = new ConfigTestHelper(dir, null);
    Document doc = h.asXml("test.xml");
    h.assertXmlText("bar", doc, "test/foo");

  }

  private static RepositorySystemSession newRepositorySystemSession(File localRepo) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    // We are not concerned with checking the _remote.repositories files
    try {
      session.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(session, new LocalRepository(localRepo)));
    } catch (NoLocalRepositoryManagerException e) {
      // This should never happen
      throw new IllegalStateException(e);
    }
    // Don't follow remote repositories in POMs
    session.setIgnoreArtifactDescriptorRepositories(true);
    // resolve pom packaging
    session.setConfigProperty(ArtifactDescriptorReaderDelegate.class.getName(), new ArtifactDescriptorReaderDelegate() {
      @Override
      public void populateResult(RepositorySystemSession session, ArtifactDescriptorResult result, Model model) {
        super.populateResult(session, result, model);
        result.getProperties().put("packaging", model.getPackaging());
      }
    });
    return session;
  }
}
