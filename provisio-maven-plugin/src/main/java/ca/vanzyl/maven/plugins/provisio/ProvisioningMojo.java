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

import ca.vanzyl.provisio.model.ProvisioArchive;
import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import ca.vanzyl.provisio.model.Runtime;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.vanzyl.provisio.MavenProvisioner;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;

@Mojo(name = "provision", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ProvisioningMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  private RepositorySystem repositorySystem;

  @Inject
  private Provisio provisio;

  @Inject
  private MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${project}")
  @Incremental(configuration = Configuration.ignore)
  protected MavenProject project;

  @Parameter(defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repositorySystemSession;

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File outputDirectory;

  @Parameter(required = true, defaultValue = "${basedir}/src/main/provisio")
  private File descriptorDirectory;

  @Parameter(defaultValue = "${session}")
  private MavenSession session;

  public void execute() throws MojoExecutionException, MojoFailureException {

    for (Runtime runtime : provisio.findDescriptors(descriptorDirectory, project)) {
      //
      // Add the ArtifactSet reference for the runtime classpath
      //
      ArtifactSet runtimeArtifacts = getRuntimeClasspathAsArtifactSet();
      ProvisioArtifact projectArtifact = projectArtifact();
      if (projectArtifact != null) {
        runtime.addArtifactReference("projectArtifact", projectArtifact);
        runtimeArtifacts.addArtifact(projectArtifact);
        //
        // If this is not a provisio-based packaging type, but we have no way to know that really. The presto-plugin
        // packaging type uses provisio but there is no way to inherit packaging types
        //
        if (!project.getPackaging().equals("jar")) {
          projectHelper.attachArtifact(project, "jar", projectArtifact.getFile());
        }
      }
      runtime.addArtifactSetReference("runtime.classpath", runtimeArtifacts);
      //
      // Provision the runtime
      //
      ProvisioningRequest request = new ProvisioningRequest();
      request.setOutputDirectory(outputDirectory);
      request.setRuntimeDescriptor(runtime);
      request.setVariables(runtime.getVariables());
      request.setManagedDependencies(provisio.getManagedDependencies(project));

      MavenProvisioner provisioner = new MavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());
      ProvisioningResult result;
      try {
        result = provisioner.provision(request);
      } catch (Exception e) {
        throw new MojoExecutionException("Error provisioning assembly.", e);
      }

      if (result.getArchives() != null) {
        if (result.getArchives().size() == 1) {
          ProvisioArchive provisioArchive = result.getArchives().get(0);
          if(project.getPackaging().equals("jar") || project.getPackaging().equals("takari-jar")) {
            projectHelper.attachArtifact(project, provisioArchive.extension(), provisioArchive.file());
          } else {
            // We have something like provisio or presto-plugin
            project.getArtifact().setFile(provisioArchive.file());
          }
        }
      }
    }
  }

  private ProvisioArtifact projectArtifact() {
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
  private ArtifactSet getRuntimeClasspathAsArtifactSet() {
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

}
