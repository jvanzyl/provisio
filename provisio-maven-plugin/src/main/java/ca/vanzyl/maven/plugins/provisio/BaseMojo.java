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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
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

  @Parameter(defaultValue = "${session}")
  protected MavenSession session;

  protected ProvisioArtifact projectArtifact() {
    ProvisioArtifact jarArtifact = null;
    //
    // We also need to definitively know what others types of runtime artifacts have been created. Right now there
    // is no real protocol for knowing what something like, say, the JAR plugin did to drop off a file somewhere. We
    // need to improve this but for now we'll look.
    //
    File jar = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-" + project.getVersion() + ".jar");
    if (jar.exists()) {
      jarArtifact = new ProvisioArtifact(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
      jarArtifact.setFile(jar);
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
    for (org.apache.maven.artifact.Artifact mavenArtifact : project.getArtifacts()) {
      if (!mavenArtifact.getScope().equals("system") && !mavenArtifact.getScope().equals("provided")) {
        artifactSet.addArtifact(new ProvisioArtifact(toArtifact(mavenArtifact)));
      }
    }
    return artifactSet;
  }

  private static Artifact toArtifact(org.apache.maven.artifact.Artifact artifact) {
    if (artifact == null) {
      return null;
    }

    String version = artifact.getVersion();
    if (version == null && artifact.getVersionRange() != null) {
      version = artifact.getVersionRange().toString();
    }

    Map<String, String> props = null;
    if (org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
      String localPath = (artifact.getFile() != null) ? artifact.getFile().getPath() : "";
      props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, localPath);
    }

    Artifact result = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getArtifactHandler().getExtension(), version, props,
      newArtifactType(artifact.getType(), artifact.getArtifactHandler()));
    result = result.setFile(artifact.getFile());

    return result;
  }

  private static ArtifactType newArtifactType(String id, ArtifactHandler handler) {
    return new DefaultArtifactType(id, handler.getExtension(), handler.getClassifier(), handler.getLanguage(), handler.isAddedToClasspath(), handler.isIncludesDependencies());
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
    if (duplicates.size() != 0) {
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
      if (artifact.getClassifier() != null && artifact.getClassifier().length() != 0) {
        dependency.setClassifier(artifact.getClassifier());
      }
      if (artifact.getExtension() != null && artifact.getExtension().length() != 0 && !artifact.getExtension().equals("jar")) {
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
    ArrayList<Dependency> sorted = new ArrayList<>(dependencies);
    sorted.sort(
            Comparator.comparing(Dependency::getScope, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Dependency::getGroupId)
                    .thenComparing(Dependency::getArtifactId)
                    .thenComparing(Dependency::getVersion, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Dependency::getClassifier, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(Dependency::getType, Comparator.nullsFirst(Comparator.naturalOrder())));
    model.setDependencies(sorted);
  }
}
