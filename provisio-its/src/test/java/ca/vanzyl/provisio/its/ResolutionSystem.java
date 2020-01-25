package ca.vanzyl.provisio.its;

import java.io.File;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import com.google.common.collect.Lists;

import io.takari.aether.connector.AetherRepositoryConnectorFactory;

public class ResolutionSystem {

  private File localRepository;
  private RepositorySystem system;
  private RepositorySystemSession session;
  private List<RemoteRepository> remoteRepositories;

  public ResolutionSystem(File localRepository) {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);    
    locator.addService(FileProcessor.class, DefaultFileProcessor.class);
    this.localRepository = localRepository;
    this.system = locator.getService(RepositorySystem.class);
    this.session = repositorySystemSession();
    this.remoteRepositories = Lists.newArrayList();
  }

  public ArtifactType getArtifactType(String typeId) {
    return session.getArtifactTypeRegistry().get(typeId);
  }

  public void remoteRepository(String remoteRepository) {
    remoteRepositories.add(remoteRepository(new Repository(remoteRepository.substring(0, remoteRepository.indexOf("//")), remoteRepository)));
  }
    
  private RemoteRepository remoteRepository(Repository r) {
    RemoteRepository.Builder builder = new RemoteRepository.Builder(r.getId(), "default", r.getUrl());
    if (r.getUsername() != null && r.getPassword() != null) {
      Authentication auth = new AuthenticationBuilder().addUsername(r.getUsername())
        .addPassword(r.getPassword()).build();
      builder.setAuthentication(auth);
    }
    return builder.build();
  }

  public List<RemoteRepository> remoteRepositories() {
    return remoteRepositories;
  }
  
  public RepositorySystem repositorySystem() {
    return system;
  }
  
  public RepositorySystemSession repositorySystemSession() {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(localRepository);
    // We are not concerned with checking the _remote.repositories files
    try {
      session.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(session, localRepo));
    } catch (NoLocalRepositoryManagerException e) {
      // This should never happen
    }
    // Don't follow remote repositories in POMs
    session.setIgnoreArtifactDescriptorRepositories(true);
    session.setTransferListener(new QuietTransferListener());
    session.setRepositoryListener(new QuietRepositoryListener());
    return session;
  }  
  
  // We're no looking to watch any output here but if we do, in fact, need to watch anything
  // we can make simple changes to these no-op implementations
  public class QuietRepositoryListener extends AbstractRepositoryListener {
  }

  public class QuietTransferListener extends AbstractTransferListener {
  }
}
