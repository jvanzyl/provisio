package io.provis.jenkins.aether;

import java.io.File;
import java.util.List;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import com.google.common.collect.Lists;

import io.takari.aether.connector.AetherRepositoryConnectorFactory;

public class ResolutionSystem {

  private RepositorySystem system;
  private ArtifactDescriptorReader descriptorReader;
  private List<RemoteRepository> remoteRepositories;

  public ResolutionSystem(File localRepository) {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(FileProcessor.class, DefaultFileProcessor.class);
    this.system = locator.getService(RepositorySystem.class);
    this.descriptorReader = locator.getService(ArtifactDescriptorReader.class);
    this.remoteRepositories = Lists.newArrayList();
  }

  public void remoteRepository(String remoteRepository) {
    if (remoteRepository == null) {
      return;
    }
    if (!remoteRepository.contains("//")) {
      throw new IllegalStateException("Bad repository url: " + remoteRepository);
    }
    remoteRepository(new Repository(remoteRepository.substring(0, remoteRepository.indexOf("//")), remoteRepository));
  }

  public void remoteRepository(Repository r) {
    remoteRepositories.add(createRepository(r));
  }

  public List<RemoteRepository> remoteRepositories() {
    return remoteRepositories;
  }

  public RepositorySystem repositorySystem() {
    return system;
  }

  public ArtifactDescriptorReader getDescriptorReader() {
    return descriptorReader;
  }

  public static RemoteRepository createRepository(Repository r) {
    RemoteRepository.Builder builder = new RemoteRepository.Builder(r.getId(), "default", r.getUrl());
    if (r.getUsername() != null && r.getPassword() != null) {
      Authentication auth = new AuthenticationBuilder().addUsername(r.getUsername())
        .addPassword(r.getPassword()).build();
      builder.setAuthentication(auth);
    }
    return builder.build();
  }

}
