package io.provis.testing;

import io.tesla.aether.connector.AetherRepositoryConnectorFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RequestTrace;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.file.FileRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.impl.RemoteRepositoryManager;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Tool for generating remote repositories for testing. It will make requests to resolve real artifacts, take the resulting
 * artifacts in resulting local repository and replace the content with the name of the artifact and generate the information
 * to make it an intact file-based remote repository. This makes it tenable to have largish remote repositories for testing
 * that don't take up a lot of space.
 * 
 * Also a demonstration of how janky Aether is. It's not an easy API for anyone to wrap their head around. It's also very easy
 * to mis-implement (no default constructor implementation just silently don't show up), and mis-configure.
 * 
 * @author Jason van Zyl
 *
 */
public class RemoteRepositoryGenerator {

  private final File localRepository;
  private final RemoteRepository remoteRepository;
  private final List<RemoteRepository> remoteRepositories;
  private final boolean forceResolutionToRemoteRepository;
  private final boolean retainChecksums;
  private final RepositorySystem system;
  private final RepositorySystemSession session;
  private final ModelBuilder modelBuilder;
  private final ArtifactResolver artifactResolver;
  private final RemoteRepositoryManager remoteRepositoryManager;

  // Don't think we need any more parameters or create a request and builder
  public RemoteRepositoryGenerator(File localRepository, String remoteRepositoryUrl, boolean retainChecksums, boolean forceResolutionToRemoteRepository) throws Exception {
    this.localRepository = localRepository;
    this.remoteRepository = new RemoteRepository.Builder("central", "default", remoteRepositoryUrl).build();
    this.remoteRepositories = Lists.newArrayList();
    this.remoteRepositories.add(this.remoteRepository);
    this.retainChecksums = retainChecksums;
    this.forceResolutionToRemoteRepository = forceResolutionToRemoteRepository;
    //
    // Lookup components required for resolution
    //
    ServiceLocator serviceLocator = serviceLocator();
    this.system = serviceLocator.getService(RepositorySystem.class);
    this.session = repositorySystemSession(this.system);
    this.modelBuilder = new DefaultModelBuilderFactory().newInstance();
    this.artifactResolver = serviceLocator.getService(ArtifactResolver.class);
    this.remoteRepositoryManager = serviceLocator.getService(RemoteRepositoryManager.class);
  }

  public void generateFromCoordinates(String... coordinates) throws Exception {
    FileUtils.deleteDirectory(localRepository);
    localRepository.mkdirs();
    for (String coordinate : coordinates) {
      resolveTransitively(coordinate);
    }
  }

  public void generateFromPomCoordinate(String coordinate) throws Exception {
    generateFromPomArtifact(new DefaultArtifact(coordinate));
  }
    
  public void generateFromPom(File pomFile) throws Exception {
    Model model = resolveModel(pomFile);
    generateFromPomArtifact(new DefaultArtifact(model.getGroupId(), model.getArtifactId(), model.getPackaging(), model.getVersion()));
  }

  public void generateFromPomArtifact(Artifact artifact) throws Exception {    
    ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
    descriptorRequest.setArtifact(artifact);
    ArtifactDescriptorResult descriptorResult = system.readArtifactDescriptor(session, descriptorRequest);
    DependencyRequest dependencyRequest = dependencyRequest(artifact);
    for (Dependency dependency : descriptorResult.getDependencies()) {
      dependencyRequest.getCollectRequest().addDependency(dependency);
    } 
    resolveTransitively(dependencyRequest);
  }  
  
  private void resolveTransitively(String coordinate) throws Exception {
    Artifact artifact = new DefaultArtifact(coordinate);
    resolveTransitively(dependencyRequest(artifact));
  }  

  private void resolveTransitively(DependencyRequest dependencyRequest) throws Exception {
    List<ArtifactResult> artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();
    for (ArtifactResult artifactResult : artifactResults) {
      // Replace the binary content with the name of the artifact
      File file = artifactResult.getArtifact().getFile();
      file.delete();
      Files.write(file.getName(), file, Charsets.UTF_8);
    }
    if (!retainChecksums) {
      java.nio.file.Files.walkFileTree(localRepository.toPath(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String fileName = file.getFileName().toString();
          if (fileName.endsWith(".md5") || fileName.endsWith(".sha1")) {
            java.nio.file.Files.delete(file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    }    
  }

  //
  // Utilities for resolving
  //    
  private DependencyRequest dependencyRequest(Artifact artifact) {    
    DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);    
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
    collectRequest.addRepository(remoteRepository);
    return new DependencyRequest(collectRequest, classpathFlter);    
  }
  
  public Model resolveModel(File pom) throws ModelBuildingException {
    RequestTrace trace = new RequestTrace(pom);
    ModelBuildingRequest modelRequest = new DefaultModelBuildingRequest();
    modelRequest.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
    modelRequest.setProcessPlugins(false);
    modelRequest.setTwoPhaseBuilding(false);
    modelRequest.setSystemProperties(toProperties(session.getUserProperties(), session.getSystemProperties()));
    //
    // The model cache and default model resolver should be injected
    //
    modelRequest.setModelCache(new DefaultModelCache());    
    modelRequest.setModelResolver(new DefaultModelResolver(session, trace.newChild(modelRequest), "bithub", artifactResolver, remoteRepositoryManager, remoteRepositories));
    modelRequest.setPomFile(pom);
    return modelBuilder.build(modelRequest).getEffectiveModel();
  }

  private DefaultRepositorySystemSession repositorySystemSession(RepositorySystem system) throws Exception {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    LocalRepository localRepo = new LocalRepository(localRepository);
    session.setTransferListener(new ConsoleTransferListener());
    session.setRepositoryListener(new ConsoleRepositoryListener());

    if (forceResolutionToRemoteRepository) {
      // If we're working against our local repository manager we want to force
      DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
      mirrorSelector.add("central", remoteRepository.getUrl(), "default", false, "external:*", "default");
      session.setMirrorSelector(mirrorSelector);
    }

    // This prevents any of the _remote.repositories files from being written, we don't need them in a remote repository
    SimpleLocalRepositoryManagerFactory f = new SimpleLocalRepositoryManagerFactory();
    session.setLocalRepositoryManager(f.newInstance(session, localRepo));

    // uncomment to generate dirty trees
    // session.setDependencyGraphTransformer( null );
    return session;
  }

  private ServiceLocator serviceLocator() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
    locator.addService(RepositoryConnectorFactory.class, FileRepositoryConnectorFactory.class);
    locator.addService(RepositoryConnectorFactory.class, AetherRepositoryConnectorFactory.class);
    locator.addService(FileProcessor.class, DefaultFileProcessor.class);
    return locator;
  }

  private Properties toProperties(Map<String, String> dominant, Map<String, String> recessive) {
    Properties props = new Properties();
    if (recessive != null) {
      props.putAll(recessive);
    }
    if (dominant != null) {
      props.putAll(dominant);
    }
    return props;
  }

  public static void main(String[] args) throws Exception {
    RemoteRepositoryGenerator repoGenerator = new RemoteRepositoryGenerator(new File("/tmp/foo10"), "http://localhost:8081/nexus/content/groups/public/", false, true);
    repoGenerator.generateFromPomCoordinate("com.facebook.presto:presto-cassandra:0.74");
  }
}
