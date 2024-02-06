/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.maven.plugins.provisio;

import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.Runtime;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
import org.eclipse.aether.util.graph.visitor.DependencyGraphDumper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseMojo
        extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  protected RepositorySystem repositorySystem;

  @Inject
  protected Provisio provisio;

  @Inject
  protected MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${project}")
  @Incremental(configuration = Configuration.ignore)
  protected MavenProject project;

  @Parameter(defaultValue = "${repositorySystemSession}")
  protected RepositorySystemSession repositorySystemSession;

  @Parameter(required = true, defaultValue = "${basedir}/src/main/provisio")
  protected File descriptorDirectory;

  protected ProvisioArtifact projectArtifact() {
    ProvisioArtifact jarArtifact = null;
    //
    // We also need to definitively know what others types of runtime artifacts have been created. Right now there
    // is no real protocol for knowing what something like, say, the JAR plugin did to drop off a file somewhere. We
    // need to improve this but for now we'll look.
    // ===
    // While this above is still true, this check below is better, in a sense it allows JAR plugin to drop off file
    // anywhere.
    //
    Artifact projectArtifact = RepositoryUtils.toArtifact(project.getArtifact());
    if (projectArtifact.getFile() != null
            && projectArtifact.getFile().getName().endsWith(".jar")
            && projectArtifact.getFile().exists()) {
      jarArtifact = new ProvisioArtifact(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
      jarArtifact.setFile(projectArtifact.getFile());
    }
    return jarArtifact;
  }

  //
  // We want to produce an artifact set the corresponds to the runtime classpath of the project. This ArtifactSet will contain:
  //
  // - runtime dependencies
  // - any artifacts produced by this build
  //
  protected ArtifactSet getRuntimeClasspathAsArtifactSet() {
    //
    // project.getArtifacts() will return us the runtime artifacts as that's the resolution scope we have requested
    // for this Mojo. I think this will be sufficient for anything related to creating a runtime.
    //
    ArtifactSet artifactSet = new ArtifactSet();
    for (Artifact mavenArtifact : resolveRuntimeScopeTransitively()) {
      artifactSet.addArtifact(new ProvisioArtifact(mavenArtifact));
    }
    return artifactSet;
  }


  /**
   * This method is in use instead of project offering mojo asked resolution scope due presence of:
   * <a href="https://issues.apache.org/jira/browse/MNG-8041">MNG-8041</a>
   */
  private List<Artifact> resolveRuntimeScopeTransitively() {
    DependencyFilter runtimeFilter = new ScopeDependencyFilter(JavaScopes.SYSTEM, JavaScopes.PROVIDED, JavaScopes.TEST);
    List<org.eclipse.aether.graph.Dependency> dependencies = project.getDependencies().stream()
            .map(d -> RepositoryUtils.toDependency(d, repositorySystemSession.getArtifactTypeRegistry()))
            .filter(d -> !JavaScopes.TEST.equals(d.getScope()))
            .collect(Collectors.toList());
    List<org.eclipse.aether.graph.Dependency> managedDependencies = Collections.emptyList();
    if (project.getDependencyManagement() != null) {
      managedDependencies = project.getDependencyManagement().getDependencies().stream()
                      .map(d -> RepositoryUtils.toDependency(d, repositorySystemSession.getArtifactTypeRegistry()))
                      .collect(Collectors.toList());
    }

    CollectRequest collectRequest = new CollectRequest();
    collectRequest.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));
    collectRequest.setRepositories(project.getRemoteProjectRepositories());
    collectRequest.setDependencies(dependencies);
    collectRequest.setManagedDependencies(managedDependencies);
    DependencyRequest request = new DependencyRequest(collectRequest, runtimeFilter);
    try {
      DependencyResult result = repositorySystem.resolveDependencies(repositorySystemSession, request);

      if (logger.isDebugEnabled() && result.getRoot() != null) {
        logger.debug("BaseMojo -- Collection result for {}", request.getCollectRequest());
        result.getRoot().accept(new DependencyGraphDumper(logger::debug));
      }

      return result.getArtifactResults().stream()
              .map(ArtifactResult::getArtifact)
              .collect(Collectors.toList());
    } catch (DependencyResolutionException e) {
      logger.error("Failed to resolve runtime dependencies", e);
      throw new RuntimeException(e);
    }
  }

  protected ProvisioningRequest getRequest(Runtime runtime)
  {
    ProvisioningRequest request = new ProvisioningRequest();
    request.setRuntimeDescriptor(runtime);
    request.setVariables(runtime.getVariables());
    request.setManagedDependencies(provisio.getManagedDependencies(project));
    return request;
  }

  protected void checkDuplicates(List<ProvisioArtifact> artifacts)
          throws MojoFailureException
  {
    Map<String, Set<String>> grouped = new HashMap<>();
    for (ProvisioArtifact artifact : artifacts) {
      String key = artifact.toVersionlessCoordinate();
      if (!grouped.containsKey(key)) {
        grouped.put(key, new HashSet<>());
      }
      grouped.get(key).add(key + ":" + artifact.getVersion());
    }
    List<String> duplicates = grouped.values()
            .stream()
            .filter(strings -> strings.size() > 1)
            .map(strings -> String.join(", ", strings))
            .collect(Collectors.toList());
    if (!duplicates.isEmpty()) {
      throw new MojoFailureException("Found different versions of the same dependency: " + String.join(", ", duplicates));
    }
  }

  protected List<Dependency> getDependencies(List<ProvisioArtifact> artifacts)
  {
    List<Dependency> dependencies = new ArrayList<>();
    for (ProvisioArtifact artifact : artifacts) {
      Dependency dependency = new Dependency();
      dependency.setGroupId(artifact.getGroupId());
      dependency.setArtifactId(artifact.getArtifactId());
      dependency.setVersion(artifact.getVersion());
      if (artifact.getClassifier() != null && !artifact.getClassifier().isEmpty()) {
        dependency.setClassifier(artifact.getClassifier());
      }
      if (artifact.getExtension() != null && !artifact.getExtension().isEmpty() && !artifact.getExtension().equals("jar")) {
        dependency.setType(artifact.getExtension());
      }
      dependencies.add(dependency);
    }
    return dependencies;
  }

  protected void mergeDependencies(Model model, List<Dependency> dependencies)
  {
    for (Dependency dependency : model.getDependencies()) {
      if (dependency.getScope() != null && !dependency.getScope().equals("compile")) {
        dependencies.add(dependency);
      }
    }
    Comparator<Dependency> comparator = Comparator.comparing(Dependency::getScope, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Dependency::getGroupId)
            .thenComparing(Dependency::getArtifactId)
            .thenComparing(Dependency::getVersion, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Dependency::getClassifier, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(Dependency::getType, Comparator.nullsFirst(Comparator.naturalOrder()));
    TreeSet<Dependency> sorted = new TreeSet<>(comparator);
    sorted.addAll(dependencies);
    model.setDependencies(new ArrayList<>(sorted));
  }
}
