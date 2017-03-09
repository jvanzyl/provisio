/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio.jenkins;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactDescriptorReader;

import io.provis.jenkins.JenkinsInstallationProvisioner;
import io.provis.jenkins.JenkinsInstallationRequest;
import io.provis.jenkins.config.Configuration;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.Archiver.ArchiverBuilder;

@Mojo(name = "provision-jenkins", defaultPhase = LifecyclePhase.PACKAGE)
public class JenkinsProvisioningMojo extends AbstractJenkinsProvisioningMojo {

  @Inject
  private RepositorySystem repositorySystem;

  @Inject
  private MavenProjectHelper projectHelper;

  @Inject
  private ArtifactDescriptorReader descriptorReader;

  @Parameter(defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repositorySystemSession;

  @Parameter(required = false, defaultValue = "true", property = "writeMasterKey")
  private boolean writeMasterKey;

  @Parameter(defaultValue = "${project.build.directory}")
  private File target;

  @Parameter(required = false)
  private File templateDirectory;

  @Parameter(required = false)
  private File webappOverrides;

  @Parameter(defaultValue = "${session}")
  private MavenSession session;

  public void execute() throws MojoExecutionException, MojoFailureException {
    JenkinsInstallationProvisioner p = JenkinsInstallationProvisioner.create(
      repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories(), descriptorReader);
    ArchiverBuilder builder = Archiver.builder();

    List<File> descriptors = descriptors();
    for (File desc : descriptors) {
      String classifier;
      String name = project.getArtifactId() + "-" + project.getVersion();
      if (descriptors.size() == 1) {
        classifier = null;
      } else {
        classifier = desc.getName().substring(0, desc.getName().lastIndexOf('.'));
        name += "-" + classifier;
      }
      File output = new File(target, name);
      File tgz = new File(target, name + ".tar.gz");

      Configuration conf = getConfig(desc);

      // collect managed dependencies
      Map<String, String> managedVersions = new HashMap<>();
      if (project.getManagedVersionMap() != null) {
        for (Artifact managed : project.getManagedVersionMap().values()) {
          managedVersions.put(managed.getGroupId() + ":" + managed.getArtifactId(), managed.getVersion());
        }
      }

      JenkinsInstallationRequest req = new JenkinsInstallationRequest(output, conf)
        .writeMasterKey(writeMasterKey)
        .configOverrides(templateDirectory)
        .webappOverrides(webappOverrides)
        .managedVersions(managedVersions);
      Archiver archiver = builder.posixLongFileMode(true).build();

      getLog().info("Bulding jenkins distro " + name);

      try {
        p.provision(req);
        archiver.archive(tgz, output);
      } catch (Exception e) {
        throw new MojoExecutionException("Cannot provision jenkins distro " + name, e);
      }

      if (classifier == null) {
        projectHelper.attachArtifact(project, "tar.gz", tgz);
      } else {
        projectHelper.attachArtifact(project, "tar.gz", classifier, tgz);
      }

    }
  }

}
