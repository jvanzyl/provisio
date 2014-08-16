package io.provis.provision;

import io.provis.model.ArtifactSet;
import io.provis.model.Lookup;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Resource;
import io.provis.provision.action.artifact.WriteToDiskAction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class DefaultMavenProvisioner implements MavenProvisioner {

  private RepositorySystem repositorySystem;
  private RepositorySystemSession repositorySystemSession;
  private List<RemoteRepository> remoteRepositories;

  public DefaultMavenProvisioner(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
    this.remoteRepositories = remoteRepositories;
  }

  public ProvisioningResult provision(ProvisioningRequest request) {

    ProvisioningResult result = new ProvisioningResult();
    ProvisioningContext context = new ProvisioningContext(request, result);
    //
    // We probably want to make sure all the operations can be done first
    //
    for (ArtifactSet fileSet : request.getModel().getArtifactSets()) {
      try {
        processArtifactSet(request, context, fileSet);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    try {
      processRuntimeActions(request, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void processArtifactSet(ProvisioningRequest request, ProvisioningContext context, ArtifactSet artifactSet) throws Exception {
    
    resolveFileSetOutputDirectory(request, context, artifactSet);
    resolveFileSetArtifacts(request, context, artifactSet);
    processArtifactsWithActions(context, artifactSet);
    resolveResourcesForArtifactSet(request, context, artifactSet);    
    processArtifactSetActions(context, artifactSet.getOutputDirectory(), artifactSet);

    if (artifactSet.getArtifactSets() != null) {
      for (ArtifactSet childFileSet : artifactSet.getArtifactSets()) {
        processArtifactSet(request, context, childFileSet);
      }
    }
  }

  private void resolveFileSetOutputDirectory(ProvisioningRequest request, ProvisioningContext context, ArtifactSet artifactSet) {
    ArtifactSet parent = artifactSet.getParent();
    if (parent != null) {
      artifactSet.setOutputDirectory(new File(parent.getOutputDirectory(), artifactSet.getDirectory()));
    } else {
      if (artifactSet.getDirectory().equals("root") || artifactSet.getDirectory().equals("/")) {
        artifactSet.setOutputDirectory(request.getOutputDirectory());
      } else {
        artifactSet.setOutputDirectory(new File(request.getOutputDirectory(), artifactSet.getDirectory()));
      }
    }    
    if (!artifactSet.getOutputDirectory().exists()) {
      artifactSet.getOutputDirectory().mkdirs();
    }
  }

  private void resolveFileSetArtifacts(ProvisioningRequest request, ProvisioningContext context, ArtifactSet artifactSet) {
    //
    // Set Parent = [a, b, c]
    // Set Child = [a, b, c, d, e, f]
    //
    // First we want to collect all the dependencies that a FileSet may yield.
    //
    // We want to use this first in a calculation of overlapping dependencies between FileSets that have a parent-->child relationship. We are making the assumption that a
    // classloader relationship will be setup along the lines of the parent-->child relationship. So we only want to place in the child's directory the artifacts that
    // are not present in the parent.
    //
    Set<ProvisioArtifact> artifacts = resolveArtifactSet(request, artifactSet);
    ArtifactSet parent = artifactSet.getParent();
    if (parent != null) {
      Set<ProvisioArtifact> parentArtifacts = artifactSet.getParent().getResolvedArtifacts();
      //Set<ProvisioArtifact> resolved = Maps.difference(artifacts, parentArtifacts).entriesOnlyOnLeft();      
      Set<ProvisioArtifact> resolved = Sets.difference(artifacts, parentArtifacts);
      artifactSet.setResolvedArtifacts(resolved);
    } else {
      artifactSet.setResolvedArtifacts(artifacts);
    }
  }

  private void resolveResourcesForArtifactSet(ProvisioningRequest request, ProvisioningContext context, ArtifactSet artifactSet) throws Exception {
    for(Resource resource : artifactSet.getResources()) {
      File source = new File(new File(request.getOutputDirectory(), "..").getCanonicalFile(), resource.getName());
      if(!source.exists()) {
        throw new RuntimeException(String.format("The specified file %s does not exist.", source));
      }
      File target = new File(artifactSet.getOutputDirectory(), resource.getName());
      Files.copy(source, target);      
    }
  }
  
  
  //
  // Process actions that apply across the entire runtime installation
  //
  private void processRuntimeActions(ProvisioningRequest request, ProvisioningContext context) throws Exception {
    for (ProvisioningAction action : request.getRuntime().getActions()) {
      configureArtifactSetAction(action, request.getOutputDirectory());
      action.execute(context);
    }    
  }

  Lookup lookup = new Lookup();

  //
  // Process actions that apply across filesets
  //
  private void processArtifactSetActions(ProvisioningContext context, File outputDirectory, ArtifactSet artifactSet) throws Exception {
  }

  //
  // Process actions that apply to artifacts
  //
  private void processArtifactsWithActions(ProvisioningContext context, ArtifactSet artifactSet) throws Exception {
    for (ProvisioArtifact artifact : artifactSet.getResolvedArtifacts()) {
      if (artifact.getActions() != null) {
        for (ProvisioningAction action : artifact.getActions()) {
          configureArtifactAction(artifact, action, artifactSet.getOutputDirectory());
          action.execute(context);
        }
      } else {
        ProvisioningAction action = new WriteToDiskAction(artifact, artifactSet.getOutputDirectory());
        action.execute(context);
      }
    }
  }

  private void configureArtifactSetAction(ProvisioningAction provisioningAction, File outputDirectory) {
    lookup.setObjectProperty(provisioningAction, "fileSetDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "outputDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "runtimeDirectory", outputDirectory);
  }

  private void configureArtifactAction(ProvisioArtifact artifact, ProvisioningAction provisioningAction, File outputDirectory) {
    lookup.setObjectProperty(provisioningAction, "artifact", artifact);
    lookup.setObjectProperty(provisioningAction, "fileSetDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "outputDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "runtimeDirectory", outputDirectory);
  }

  //
  // Resolving artifact sets
  //
  public Set<ProvisioArtifact> resolveArtifactSet(ProvisioningRequest provisioningRequest, ArtifactSet fileSet) {

    CollectRequest request = new CollectRequest();
    //
    // Resolve versions
    //
    Map<String, ProvisioArtifact> artifacts = fileSet.getArtifactMap();

    for (ProvisioArtifact artifact : artifacts.values()) {
      //
      // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
      //

      // ArtifactType
      //
      // String id 
      // String extension 
      // String classifier 
      // String language
      // boolean constitutesBuildPath
      // boolean includesDependencies (self-contained so don't attempt to download anything described in the dependency descriptor (POM)
      //
      ArtifactType type = null;

      if (artifact.getExtension().equals("tar.gz")) {
        type = new DefaultArtifactType("tar.gz", "tar.gz", "", "packaging", false, true);
      } else if (artifact.getExtension().equals("zip")) {
        type = new DefaultArtifactType("zip", "zip", "", "packaging", false, true);
      }

      //
      //TODO: Inside Maven this is not null but it should be ??? There is nothing in the type registry for it.
      //
      if (getArtifactType(artifact.getExtension()) == null) {
        //
        // We are dealing with artifacts that have no entry in the default artifact type registry. These are typically
        // archive types and we only want to retrieve the artifact itself so we set the artifact type accordingly.
        //
        type = new DefaultArtifactType(artifact.getExtension(), artifact.getExtension(), "", "unknown", false, true);
      }

      if (type != null) {
        artifact.setProperties(type.getProperties());
      }

      Dependency dependency = new Dependency(artifact, "runtime");
      request.addDependency(dependency);
    }

    //
    // Add an exclude filter if necessary
    //
    DependencyRequest dependencyRequest = new DependencyRequest(request, null);
    if (fileSet.getExcludes() != null) {
      dependencyRequest.setFilter(new ExclusionsDependencyFilter(fileSet.getExcludes()));
    }

    for (String coordinate : provisioningRequest.getManagedDependencies()) {
      Artifact artifact = new ProvisioArtifact(coordinate);
      request.addManagedDependency(new Dependency(artifact, "runtime"));
    }

    //
    // Treat the parent's resolved artifacts as set of managed dependencies for the child
    //
    if (fileSet.getParent() != null && fileSet.getParent().getResolvedArtifacts() != null) {
      for (Artifact artifact : fileSet.getParent().getResolvedArtifacts()) {
        request.addManagedDependency(new Dependency(artifact, "runtime"));
      }
    }

    List<Artifact> resultArtifacts;
    try {
      resultArtifacts = resolveArtifacts(dependencyRequest);
    } catch (DependencyResolutionException e) {
      throw new ProvisioningException(e.getMessage(), e);
    }

    Map<String, ProvisioArtifact> artifactMapKeyedByGa = new HashMap<String, ProvisioArtifact>();
    Set<ProvisioArtifact> resolvedArtifacts = Sets.newHashSet();
    for (Artifact a : resultArtifacts) {
      String ga = a.getGroupId() + ":" + a.getArtifactId();
      if (a instanceof ProvisioArtifact) {
        artifactMapKeyedByGa.put(ga, (ProvisioArtifact) a);
        resolvedArtifacts.add((ProvisioArtifact) a);
      } else {
        artifactMapKeyedByGa.put(ga, new ProvisioArtifact(a));
        resolvedArtifacts.add(new ProvisioArtifact(a));
      }
    }

    fileSet.setResolvedArtifacts(resolvedArtifacts);

    return resolvedArtifacts;
  }

  public List<Artifact> resolveArtifacts(String coordinate) throws DependencyResolutionException {
    return resolveArtifacts(new DefaultArtifact(coordinate));
  }

  public List<Artifact> resolveArtifacts(Artifact artifact) throws DependencyResolutionException {
    DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter(JavaScopes.RUNTIME);
    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRoot(new Dependency(artifact, JavaScopes.RUNTIME));
    for (RemoteRepository remoteRepository : remoteRepositories) {
      collectRequest.addRepository(remoteRepository);
    }
    DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFlter);
    return resolveArtifacts(dependencyRequest);
  }

  public List<Artifact> resolveArtifacts(DependencyRequest request) throws DependencyResolutionException {
    //
    // We are attempting to encapsulate everything about resolution with this library. The dependency request requires
    // the collect request to have repositories set but this is all injected within this component so we have to set them.
    //
    CollectRequest collectRequest = request.getCollectRequest();
    if (collectRequest.getRepositories() == null || collectRequest.getRepositories().isEmpty()) {
      for (RemoteRepository remoteRepository : remoteRepositories) {
        collectRequest.addRepository(remoteRepository);
      }
    }
    DependencyResult result = repositorySystem.resolveDependencies(repositorySystemSession, request);
    List<Artifact> artifacts = new ArrayList<Artifact>();
    for (ArtifactResult ar : result.getArtifactResults()) {
      artifacts.add(ar.getArtifact());
    }
    return artifacts;
  }

  public ArtifactType getArtifactType(String typeId) {
    return repositorySystemSession.getArtifactTypeRegistry().get(typeId);
  }

}
