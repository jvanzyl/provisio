package io.provis.provision;

import io.provis.model.Action;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioContext;
import io.provis.provision.action.artifact.ArtifactMetadataGleaner;
import io.provis.provision.action.artifact.WriteToDiskAction;
import io.provis.provision.model.ProvisoArtifactMetadata;
import io.tesla.aether.TeslaAether;
import io.tesla.aether.internal.DefaultTeslaAether;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.sisu.Nullable;

import com.google.common.collect.Maps;

@Named
@Singleton
public class DefaultProvisioner implements Provisioner {

  private TeslaAether aether;
  private VersionMapFromPom versionMapFromPom;
  private ArtifactMetadataGleaner artifactMetadataGleaner;

  @Inject
  public DefaultProvisioner(/*TeslaAether aether, */ VersionMapFromPom versionMapFromPom, ArtifactMetadataGleaner artifactMetadataGleaner) {
    this.aether = new DefaultTeslaAether();
    this.versionMapFromPom = versionMapFromPom;
    this.artifactMetadataGleaner = artifactMetadataGleaner;
  }

  public ProvisioningResult provision(ProvisioningRequest request) {

    ProvisioContext context = new ProvisioContext();
    
    //
    // We probably want to make sure all the operations can be done first
    //

    for (ArtifactSet fileSet : request.getRuntimeAssembly().getFileSets()) {
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

    return new ProvisioningResult(request.getRuntimeAssembly());
  }

  private void processFileSet(ProvisioningRequest request, ProvisioContext context, ArtifactSet fileSet) throws Exception {

    if(!fileSet.getActualOutputDirectory().exists()) {
      fileSet.getActualOutputDirectory().mkdirs();
    }
        
    resolveFileSetOutputDirectory(request, context, fileSet);
    resolveFileSetArtifacts(request, context, fileSet);
    processArtifactsWithActions(context, fileSet.getResolvedArtifacts().values(), fileSet.getActualOutputDirectory());
    processFileSetActions(context, fileSet.getActualOutputDirectory(), fileSet);

    if (fileSet.getFileSets() != null) {
      for (ArtifactSet childFileSet : fileSet.getFileSets()) {
        processFileSet(request, context, childFileSet);
      }
    }
  }

  private void resolveFileSetOutputDirectory(ProvisioningRequest request, ProvisioContext context, ArtifactSet fileSet) {
    ArtifactSet parent = fileSet.getParent();
    if (parent != null) {
      fileSet.setActualOutputDirectory(new File(parent.getActualOutputDirectory(), fileSet.getDirectory()));
    } else {
      if (fileSet.getDirectory().equals("root")) {
        fileSet.setActualOutputDirectory(request.getOutputDirectory());
      } else {
        fileSet.setActualOutputDirectory(new File(request.getOutputDirectory(), fileSet.getDirectory()));
      }      
    }
  }

  private void resolveFileSetArtifacts(ProvisioningRequest request, ProvisioContext context, ArtifactSet fileSet) {
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
    Map<String, ProvisioArtifact> artifacts = resolveFileSet(request, fileSet);
    ArtifactSet parent = fileSet.getParent();
    if (parent != null) {
      Map<String, ProvisioArtifact> parentArtifacts = fileSet.getParent().getResolvedArtifacts();
      Map<String, ProvisioArtifact> resolved = Maps.difference(artifacts, parentArtifacts).entriesOnlyOnLeft();
      fileSet.setResolvedArtifacts(resolved);
    } else {
      fileSet.setResolvedArtifacts(artifacts);
    }
  }

  //
  // Process actions that apply across the entire runtime installation
  //
  private void processRuntimeActions(ProvisioningRequest request, ProvisioContext context) throws Exception {
    for (Action action : request.getRuntimeAssembly().getActions()) {
      action.execute(context);
    }
  }

  //
  // Process actions that apply across filesets
  //
  private void processFileSetActions(ProvisioContext context, File outputDirectory, ArtifactSet fileSet) throws Exception {
    for (Action action : fileSet.getActions()) {
      action.execute(context);
    }
  }

  //
  // Process actions that apply to artifacts
  //
  private void processArtifactsWithActions(ProvisioContext context, Collection<ProvisioArtifact> artifacts, File outputDirectory) throws Exception {

    for (ProvisioArtifact artifact : artifacts) {
      //
      // Find any actions specified in the metadata of the artifact
      //
      ProvisoArtifactMetadata artifactMetadata = artifactMetadataGleaner.gleanMetadata(artifact);
      List<Action> mergedArtifactActions = mergeActionsAndSetDefaults(artifact, artifactMetadata, outputDirectory);

      for (Action action : mergedArtifactActions) {
        action.execute(context);
      }

      if (artifactMetadata != null) {
        if (artifactMetadata.getDirectoryActions() != null) {
          for (Action action : artifactMetadata.getArtifactActions()) {
            action.execute(context);
          }
        }
      }
    }
  }

  private List<Action> mergeActionsAndSetDefaults(ProvisioArtifact artifact, ProvisoArtifactMetadata artifactMetadata, File outputDirectory) {

    List<Action> actions = new ArrayList<Action>();

    for (Action action : artifact.getActions()) {
      actions.add(action);
    }

    if (artifactMetadata != null && artifactMetadata.getArtifactActions() != null) {
      for (Action action : artifactMetadata.getArtifactActions()) {
        actions.add(action);
      }
    }

    //
    // If no other actions have been set by default we want to write the artifact to disk.
    //
    if (actions.isEmpty()) {
      actions.add(new WriteToDiskAction(artifact, outputDirectory));
    }

    return actions;
  }
  
  //
  // Resolving artifact sets
  //
  public Map<String, ProvisioArtifact> resolveFileSet(ProvisioningRequest provisioningRequest, ArtifactSet fileSet) {
    
    CollectRequest request = new CollectRequest();
    //
    // Resolve versions
    //
    Map<String, ProvisioArtifact> artifacts;

    artifacts = fileSet.getArtifactMap();
    
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
      
      if(aether.getArtifactType(artifact.getExtension()) == null) {
        //
        // We are dealing with artifacts that have no entry in the default artifact type registry. These are typically
        // archive types and we only want to retrieve the artifact itself so we set the artifact type accordingly.
        //
        ArtifactType type;
        if (artifact.getExtension().equals("tar.gz")) {
          type = new DefaultArtifactType("tar.gz", "tar.gz", "", "packaging", false, true);
        } else if (artifact.getExtension().equals("zip")) {
          type = new DefaultArtifactType("zip", "zip", "", "packaging", false, true);        
        } else {
          type = new DefaultArtifactType(artifact.getExtension(), artifact.getExtension(), "", "unknown", false, true);        
        }
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

    if(provisioningRequest.getRuntimeAssembly().getVersionMap() != null) {
      try {
        Map<String,String> versionMap = versionMapFromPom.versionMap(provisioningRequest.getRuntimeAssembly().getVersionMap());
        for(String key : versionMap.keySet()) {
          Artifact artifact = new ProvisioArtifact(key + ":" + versionMap.get(key));
          request.addManagedDependency(new Dependency(artifact, "runtime"));              
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }      
    }
        
    //
    // Treat the parent's resolved artifacts as set of managed dependencies for the child
    //
    if (fileSet.getParent() != null && fileSet.getParent().getResolvedArtifacts() != null) {
      for (Artifact artifact : fileSet.getParent().getResolvedArtifacts().values()) {
        request.addManagedDependency(new Dependency(artifact, "runtime"));
      }
    }
    
    List<Artifact> resultArtifacts;
    try {
      resultArtifacts = aether.resolveArtifacts(dependencyRequest);
    } catch (DependencyResolutionException e) {
      throw new ProvisioningException(e.getMessage(), e);
    }

    Map<String, ProvisioArtifact> artifactMapKeyedByGa = new HashMap<String, ProvisioArtifact>();
    for (Artifact a : resultArtifacts) {
      String ga = a.getGroupId() + ":" + a.getArtifactId();
      if (a instanceof ProvisioArtifact) {
        artifactMapKeyedByGa.put(ga, (ProvisioArtifact) a);
      } else {
        artifactMapKeyedByGa.put(ga, new ProvisioArtifact(a));
      }
    }

    fileSet.setResolvedArtifacts(artifactMapKeyedByGa);

    return artifactMapKeyedByGa;
  }
}
