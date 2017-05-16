/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

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

import io.provis.MavenProvisioner;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Runtime;
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
    String projectPackaging = project.getPackaging();
    for (Runtime runtime : provisio.findDescriptors(descriptorDirectory, project)) {
      // Add the ArtifactSet reference for the runtime classpath
      ArtifactSet runtimeArtifacts = getRuntimeClasspathAsArtifactSet();
      ProvisioArtifact projectArtifact = projectArtifact();
      if (projectArtifact != null) {
        runtime.addArtifactReference("projectArtifact", projectArtifact);
        runtimeArtifacts.addArtifact(projectArtifact);
        // Deal with the case of a non-JAR lifecycle where the JAR is needed in the reactor and externally in addition to the
        // provisio based artifact
        if (!projectPackaging.equals("jar")) {
          projectHelper.attachArtifact(project, "jar", projectArtifact.getFile());
        }
      }
      runtime.addArtifactSetReference("runtime.classpath", runtimeArtifacts);
      // Provision the runtime
      ProvisioningRequest request = new ProvisioningRequest();
      if(runtime.getOutputDirectory() != null) {
        request.setOutputDirectory(new File(runtime.getOutputDirectory()));        
      } else {        
        request.setOutputDirectory(outputDirectory);
      }
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
          File file = result.getArchives().get(0);
          if (projectPackaging.equals("jar") || projectPackaging.equals("war")) {
            projectHelper.attachArtifact(project, "tar.gz", file);
          } else {
            project.getArtifact().setFile(file);            
          }
        }
      }
    }
  }

  private ProvisioArtifact projectArtifact() {
    ProvisioArtifact projectArtifact = null;
    //
    // We also need to definitively know what others types of runtime artifacts have been created. Right now there
    // is no real protocol for knowing what something like, say, the JAR plugin did to drop off a file somewhere. We
    // need to improve this but for now we'll look.
    //
    File jar = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-" + project.getVersion() + ".jar");
    if (jar.exists()) {
      projectArtifact = new ProvisioArtifact(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
      projectArtifact.setFile(jar);
    }
    File war = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-" + project.getVersion() + ".war");
    if (war.exists()) {
      projectArtifact = new ProvisioArtifact(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
      projectArtifact.setFile(war);
    }

    return projectArtifact;
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
