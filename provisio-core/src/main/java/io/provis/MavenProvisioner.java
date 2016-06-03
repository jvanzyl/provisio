/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.provis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.provis.action.artifact.WriteToDiskAction;
import io.provis.model.ArtifactSet;
import io.provis.model.Directory;
import io.provis.model.FileSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Resource;
import io.provis.model.ResourceSet;
import io.provis.model.Runtime;

public class MavenProvisioner {

  private RepositorySystem repositorySystem;
  private RepositorySystemSession repositorySystemSession;
  private List<RemoteRepository> remoteRepositories;

  public MavenProvisioner(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteRepositories) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
    this.remoteRepositories = remoteRepositories;
  }

  public ProvisioningResult provision(ProvisioningRequest request) throws Exception {
    ProvisioningResult result = new ProvisioningResult(request);
    ProvisioningContext context = new ProvisioningContext(request, result);

    processArtifactSets(context);
    processResourceSets(context);
    processFileSets(context);
    processRuntimeActions(context);

    return result;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // ArtifactSets
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void processArtifactSets(ProvisioningContext context) throws Exception {
    for (ArtifactSet artifactSet : context.getRequest().getRuntimeModel().getArtifactSets()) {
      processArtifactSet(context, artifactSet);
    }
  }

  private void processArtifactSet(ProvisioningContext context, ArtifactSet artifactSet) throws Exception {
    resolveArtifactSetOutputDirectory(context, artifactSet);
    resolveArtifactSet(context, artifactSet);
    processArtifactsWithActions(context, artifactSet);
    processArtifactSetActions(context, artifactSet);
    //
    // Child ArtifactSets
    //
    if (artifactSet.getArtifactSets() != null) {
      for (ArtifactSet childFileSet : artifactSet.getArtifactSets()) {
        processArtifactSet(context, childFileSet);
      }
    }
  }

  private void resolveArtifactSetOutputDirectory(ProvisioningContext context, ArtifactSet artifactSet) {
    ArtifactSet parent = artifactSet.getParent();
    if (parent != null) {
      artifactSet.setOutputDirectory(new File(parent.getOutputDirectory(), artifactSet.getDirectory()));
    } else {
      if (artifactSet.getDirectory().equals("root") || artifactSet.getDirectory().equals("/")) {
        artifactSet.setOutputDirectory(context.getRequest().getOutputDirectory());
      } else {
        artifactSet.setOutputDirectory(new File(context.getRequest().getOutputDirectory(), artifactSet.getDirectory()));
      }
    }
    if (!artifactSet.getOutputDirectory().exists()) {
      artifactSet.getOutputDirectory().mkdirs();
    }
  }

  //
  // Process actions that apply across filesets
  //
  private void processArtifactSetActions(ProvisioningContext context, ArtifactSet artifactSet) throws Exception {}

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

  //
  // Resolving artifact sets
  //
  private Set<ProvisioArtifact> resolveArtifactSet(ProvisioningContext context, ArtifactSet artifactSet) {
    //
    // Resolve versions
    //
    List<ProvisioArtifact> artifacts;
    if (artifactSet.getReference() != null) {
      Runtime runtime = context.getRequest().getRuntimeModel();
      if (runtime.getArtifactSetReferences() == null) {
        throw new RuntimeException(String.format("The reference '%s' is being requested but the artifactSet references are null.", artifactSet.getReference()));
      }
      ArtifactSet referenceArtifactSet = runtime.getArtifactSetReferences().get(artifactSet.getReference());
      if (referenceArtifactSet == null) {
        throw new RuntimeException(String.format("The is no '%s' artifactSet reference available.", artifactSet.getReference()));
      }
      artifacts = referenceArtifactSet.getArtifacts();
    } else {
      artifacts = artifactSet.getArtifacts();
    }

    //
    // If the user happens to create an artifactSet in the configuration and then leaves it empty, like the "ext" configuration below:
    //
    // <artifactSet to="lib">
    //   <artifact id="ch.qos.logback:logback-core:${logbackVersion}"/>
    //   <artifact id="ch.qos.logback:logback-classic:${logbackVersion}"/>
    //   <artifactSet to="ext">
    //   </artifactSet>
    // </artifactSet>
    //
    Set<ProvisioArtifact> parentResolved = artifactSet.getParent() != null ? artifactSet.getParent().getResolvedArtifacts() : Sets.newHashSet();
    Set<ProvisioArtifact> resolvedArtifacts = resolveArtifacts(context, artifacts, parentResolved, artifactSet.getExcludes());

    artifactSet.setResolvedArtifacts(resolvedArtifacts);

    //
    // Set Parent = [a, b, c]
    // Set Child = [a, b, c, d, e, f]
    //
    // First we want to collect all the dependencies that an ArtifactSet may yield.
    //
    // We want to use this first in a calculation of overlapping dependencies between ArtifactSets that have a parent-->child relationship. We are making the assumption that a
    // classloader relationship will be setup along the lines of the parent-->child relationship. So we only want to place in the child's directory the artifacts that
    // are not present in the parent.
    //
    ArtifactSet parent = artifactSet.getParent();
    if (parent != null) {
      Set<ProvisioArtifact> parentArtifacts = artifactSet.getParent().getResolvedArtifacts();
      //
      // contained by childArtifacts and not contained in parentArtifacts
      //
      Set<ProvisioArtifact> childResolvedArtifacts = Sets.difference(resolvedArtifacts, parentArtifacts);
      artifactSet.setResolvedArtifacts(childResolvedArtifacts);
    } else {
      artifactSet.setResolvedArtifacts(resolvedArtifacts);
    }

    return resolvedArtifacts;
  }

  public Set<ProvisioArtifact> resolveArtifact(ProvisioningContext context, ProvisioArtifact artifact) {
    return resolveArtifacts(context, ImmutableList.of(artifact), Sets.<ProvisioArtifact>newHashSet(), Lists.<io.provis.model.Exclusion>newArrayList());
  }

  private Set<ProvisioArtifact> resolveArtifacts(ProvisioningContext context, List<ProvisioArtifact> artifacts, Set<ProvisioArtifact> managedArtifacts, List<io.provis.model.Exclusion> excludes) {
    CollectRequest request = new CollectRequest();
    //
    // We need to defend against a null set
    //
    if (artifacts == null) {
      artifacts = Lists.newArrayList();
    }

    if (managedArtifacts == null) {
      managedArtifacts = Sets.newHashSet();
    }

    //
    // Artifacts that have been handed to us that have been resolved or provided locally
    //
    List<ProvisioArtifact> providedArtifacts = Lists.newArrayList();
    for (ProvisioArtifact artifact : artifacts) {
      if (artifact.getReference() != null) {
        Runtime runtime = context.getRequest().getRuntimeModel();
        if (runtime.getArtifactReferences() == null) {
          throw new RuntimeException(String.format("The reference '%s' is being requested but the artifact references are null.", artifact.getReference()));
        }
        ProvisioArtifact referenceArtifact = runtime.getArtifactReferences().get(artifact.getReference());
        if (referenceArtifact == null) {
          throw new RuntimeException(String.format("The is no '%s' artifact reference available.", artifact.getReference()));
        }
        // We need a way to copy the artifact
        if (artifact.getName() != null) {
          referenceArtifact.setName(artifact.getName());
        }
        providedArtifacts.add(referenceArtifact);
        continue;
      }
      if (artifact.getFile() != null) {
        providedArtifacts.add(artifact);
        continue;
      }
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
      } else if (artifact.getExtension().equals("war")) {
        type = new DefaultArtifactType("war", "war", "", "packaging", false, true);
      } else if (artifact.getExtension().equals("hpi")) {
        type = new DefaultArtifactType("hpi", "hpi", "", "packaging", false, true);
      } else if (artifact.getExtension().equals("jpi")) {
        type = new DefaultArtifactType("jpi", "jpi", "", "packaging", false, true);
      }
      //
      // TODO: Inside Maven this is not null but it should be ??? There is nothing in the type registry for it.
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
      //
      // Equivalent of something like:
      //
      // <project>
      //   ...
      //   <dependencies>
      //     <dependency>
      //       <groupId>org.apache.maven</groupId>
      //       <artifactId>maven-core</artifactId>
      //       <version>3.3.9</version>
      //       <exclusions>
      //         <exclusion>
      //           <groupId>org.codehaus.plexus</groupId>
      //           <artifactId>plexus-utils</artifactId>
      //         </exclusion>
      //       </exclusions>
      //     </dependency>
      //   </dependencies>
      // </project>
      //
      if (artifact.getExclusions() != null) {
        Set<Exclusion> exclusions = Sets.newHashSet();
        for (String exclusion : artifact.getExclusions()) {
          String[] ga = StringUtils.split(exclusion, ":");
          if (ga.length == 2) {
            exclusions.add(new Exclusion(ga[0], ga[1], "*", "*"));
          } else if (ga.length == 1) {
            exclusions.add(new Exclusion(null, ga[0], "*", "*"));
          }
        }
        dependency = dependency.setExclusions(exclusions);
      }
      //request.setRoot(dependency);
      request.addDependency(dependency);
    }
    //
    // Add an exclude filter if necessary
    //
    DependencyRequest dependencyRequest = new DependencyRequest(request, null);
    if (excludes != null) {
      List<String> exclusions = Lists.newArrayList();
      for (io.provis.model.Exclusion exclusion : excludes) {
        exclusions.add(exclusion.getId());
      }
      dependencyRequest.setFilter(new ExclusionsDependencyFilter(exclusions));
    }

    for (String coordinate : context.getRequest().getManagedDependencies()) {
      Artifact artifact = new ProvisioArtifact(coordinate);
      request.addManagedDependency(new Dependency(artifact, "runtime"));
    }

    //
    // Treat the parent's resolved artifacts as set of managed dependencies for the child
    //
    for (Artifact artifact : managedArtifacts) {
      request.addManagedDependency(new Dependency(artifact, "runtime"));
    }

    List<Artifact> resultArtifacts;
    try {
      resultArtifacts = resolveArtifacts(dependencyRequest);
      //
      // We need to add back in the artifacts that have already been provided
      //
      resultArtifacts.addAll(providedArtifacts);
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

    return resolvedArtifacts;
  }

  private List<Artifact> resolveArtifacts(DependencyRequest request) throws DependencyResolutionException {
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

  private ArtifactType getArtifactType(String typeId) {
    return repositorySystemSession.getArtifactTypeRegistry().get(typeId);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // ResourceSets
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void processResourceSets(ProvisioningContext context) throws Exception {
    List<ResourceSet> resourceSets = context.getRequest().getRuntime().getResourceSets();
    if (resourceSets != null) {
      for (ResourceSet resourceSet : resourceSets) {
        for (Resource resource : resourceSet.getResources()) {
          File source = new File(resource.getName());
          if (!source.exists()) {
            throw new RuntimeException(String.format("The specified file %s does not exist.", source));
          }
          File target = new File(context.getRequest().getOutputDirectory(), source.getName());
          copy(source, target);
        }
      }
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // FileSets
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static Joiner joiner = Joiner.on(',').skipNulls();

  private void processFileSets(ProvisioningContext context) throws Exception {
    List<FileSet> fileSets = context.getRequest().getRuntime().getFileSets();
    if (fileSets != null) {
      for (FileSet fileSet : fileSets) {
        //
        // Files
        //
        for (io.provis.model.File file : fileSet.getFiles()) {
          if (file.getTouch() != null) {
            File target = new File(new File(context.getRequest().getOutputDirectory(), fileSet.getDirectory()), file.getTouch());
            if (!target.getParentFile().exists()) {
              target.getParentFile().mkdirs();
            }
            Files.createFile(target.toPath());
          } else {
            File source = new File(file.getPath());
            if (!source.exists()) {
              throw new RuntimeException(String.format("The specified file %s does not exist.", source));
            }
            File target = new File(new File(context.getRequest().getOutputDirectory(), fileSet.getDirectory()), source.getName());
            copy(source, target);
          }
        }
        //
        // Directories
        //
        for (Directory directory : fileSet.getDirectories()) {
          File sourceDirectory = new File(directory.getPath());
          File targetDirectory = new File(context.getRequest().getOutputDirectory(), fileSet.getDirectory());
          copyDirectoryStructure(sourceDirectory, targetDirectory, directory.getIncludes(), directory.getExcludes());
        }
      }
    }
  }

  private void copyDirectoryStructure(File sourceDirectory, File targetDirectory, List<String> includes, List<String> excludes) throws IOException {
    String includesString = null;
    if (includes != null && !includes.isEmpty()) {
      includesString = joiner.join(includes);
    }
    String excludesString = null;
    if (excludes != null && !excludes.isEmpty()) {
      excludesString = joiner.join(excludes);
    }
    List<String> relativePaths = FileUtils.getFileNames(sourceDirectory, includesString, excludesString, false);
    for (String relativePath : relativePaths) {
      File source = new File(sourceDirectory, relativePath);
      File target = new File(targetDirectory, relativePath);
      copy(source, target);
    }
  }

  private void copy(File source, File target) throws IOException {
    if (!target.getParentFile().exists()) {
      target.getParentFile().mkdirs();
    }
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //
  // Actions
  //
  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private void processRuntimeActions(ProvisioningContext context) throws Exception {
    List<ProvisioningAction> runtimeActions = context.getRequest().getRuntime().getActions();
    if (runtimeActions != null) {
      for (ProvisioningAction action : runtimeActions) {
        configureArtifactSetAction(action, context.getRequest().getOutputDirectory());
        action.execute(context);
      }
    }
  }

  // Configuring Actions, this needs to change

  Lookup lookup = new Lookup();

  private void configureArtifactSetAction(ProvisioningAction provisioningAction, File outputDirectory) {
    lookup.setObjectProperty(provisioningAction, "fileSetDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "outputDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "runtimeDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "provisioner", this);
  }

  private void configureArtifactAction(ProvisioArtifact artifact, ProvisioningAction provisioningAction, File outputDirectory) {
    lookup.setObjectProperty(provisioningAction, "artifact", artifact);
    lookup.setObjectProperty(provisioningAction, "fileSetDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "outputDirectory", outputDirectory);
    lookup.setObjectProperty(provisioningAction, "provisioner", this);
  }
}
