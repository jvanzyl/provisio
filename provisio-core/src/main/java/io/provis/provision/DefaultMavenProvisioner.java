package io.provis.provision;

import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.model.v2.Action;
import io.provis.model.v2.ArtifactSet;
import io.provis.model.v2.Lookup;
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

public class DefaultMavenProvisioner implements MavenProvisioner {

  private Map<String, ProvisioningAction> actions;
  private RepositorySystem repositorySystem;
  private RepositorySystemSession repositorySystemSession;
  private List<RemoteRepository> remoteRepositories;

  public DefaultMavenProvisioner(Map<String, ProvisioningAction> actions, RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) {
    this.actions = actions;
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
    this.remoteRepositories = remoteRepositories;
  }

  public ProvisioningResult provision(ProvisioningRequest request) {

    ProvisioningContext context = new ProvisioningContext();
    context.setVariables(request.getVariables());
    //
    // We probably want to make sure all the operations can be done first
    //
    for (ArtifactSet fileSet : request.getModel().getArtifactSets()) {
      try {
        processFileSet(request, context, fileSet);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    try {
      processRuntimeActions(request, context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return new ProvisioningResult(request.getRuntime());
  }

  private void processFileSet(ProvisioningRequest request, ProvisioningContext context, ArtifactSet artifactSet) throws Exception {

    artifactSet.setOutputDirectory(new File(request.getOutputDirectory(), artifactSet.getDirectory()));

    if (!artifactSet.getOutputDirectory().exists()) {
      artifactSet.getOutputDirectory().mkdirs();
    }

    resolveFileSetOutputDirectory(request, context, artifactSet);
    resolveFileSetArtifacts(request, context, artifactSet);
    processArtifactsWithActions(context, artifactSet);
    processFileSetActions(context, artifactSet.getOutputDirectory(), artifactSet);

    if (artifactSet.getArtifactSets() != null) {
      for (ArtifactSet childFileSet : artifactSet.getArtifactSets()) {
        processFileSet(request, context, childFileSet);
      }
    }
  }

  private void resolveFileSetOutputDirectory(ProvisioningRequest request, ProvisioningContext context, ArtifactSet fileSet) {
    ArtifactSet parent = fileSet.getParent();
    if (parent != null) {
      fileSet.setOutputDirectory(new File(parent.getOutputDirectory(), fileSet.getDirectory()));
    } else {
      if (fileSet.getDirectory().equals("root")) {
        fileSet.setOutputDirectory(request.getOutputDirectory());
      } else {
        fileSet.setOutputDirectory(new File(request.getOutputDirectory(), fileSet.getDirectory()));
      }
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

  //
  // Process actions that apply across the entire runtime installation
  //
  private void processRuntimeActions(ProvisioningRequest request, ProvisioningContext context) throws Exception {
  }

  Lookup lookup = new Lookup();

  //
  // Process actions that apply across filesets
  //
  private void processFileSetActions(ProvisioningContext context, File outputDirectory, ArtifactSet fileSet) throws Exception {
    for (Action action : fileSet.getActions()) {
      ProvisioningAction pa = actions.get(action.getId());
      configureArtifactSetAction(pa, outputDirectory, fileSet.getDirectory());
      pa.execute(context);
    }
  }

  //
  // Process actions that apply to artifacts
  //
  private void processArtifactsWithActions(ProvisioningContext context, ArtifactSet artifactSet) throws Exception {
    for (ProvisioArtifact artifact : artifactSet.getResolvedArtifacts()) {
      if (artifact.getModelArtifact() != null) {
        if (artifact.getModelArtifact().getActions() != null) {
          for (Action action : artifact.getModelArtifact().getActions()) {
            ProvisioningAction provisioningAction = actions.get(action.getId());
            configureArtifactAction(artifact, action, provisioningAction, artifactSet.getOutputDirectory(), artifactSet.getDirectory());
            provisioningAction.execute(context);
          }
        } else {
          ProvisioningAction provisioningAction = new WriteToDiskAction(artifact, artifactSet.getOutputDirectory());
          provisioningAction.execute(context);
        }
      } else {
        ProvisioningAction provisioningAction = new WriteToDiskAction(artifact, artifactSet.getOutputDirectory());
        provisioningAction.execute(context);
      }
    }
  }

  private void configureArtifactSetAction(ProvisioningAction action, File outputDirectory, String directory) {
    lookup.setObjectProperty(action, "fileSetDirectory", new File(outputDirectory, directory));
    lookup.setObjectProperty(action, "outputDirectory", new File(outputDirectory, directory));
    lookup.setObjectProperty(action, "runtimeDirectory", outputDirectory);
  }

  private void configureArtifactAction(ProvisioArtifact artifact, Action action, ProvisioningAction provisioningAction, File outputDirectory, String directory) {
    lookup.setObjectProperty(provisioningAction, "artifact", artifact);
    lookup.setObjectProperty(provisioningAction, "fileSetDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "outputDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "runtimeDirectory", outputDirectory);
    if (action.getParameters() != null) {
      for (Map.Entry<String, String> entry : action.getParameters().entrySet()) {
        Object value = entry.getValue();
        if(entry.getValue().equals("true") || entry.getValue().equals("false")) {
          value = Boolean.valueOf(entry.getValue());
        }        
        lookup.setObjectProperty(provisioningAction, entry.getKey(), value);
      }
    }
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
      System.out.println(">> " + ga);
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
