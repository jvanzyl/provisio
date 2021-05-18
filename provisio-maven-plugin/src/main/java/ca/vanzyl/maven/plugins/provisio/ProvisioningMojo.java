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

import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import ca.vanzyl.provisio.model.Runtime;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import ca.vanzyl.provisio.MavenProvisioner;

@Mojo(name = "provision", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ProvisioningMojo extends BaseMojo {

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File outputDirectory;

  public void execute() throws MojoExecutionException {
    MavenProvisioner provisioner = new MavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());

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
      ProvisioningRequest request = getRequest(runtime);
      request.setOutputDirectory(outputDirectory);

      ProvisioningResult result;
      try {
        result = provisioner.provision(request);
      } catch (Exception e) {
        throw new MojoExecutionException("Error provisioning assembly.", e);
      }

      if (result.getArchives() == null || result.getArchives().size() != 1) {
        continue;
      }
      ProvisioArchive provisioArchive = result.getArchives().get(0);
      if (project.getPackaging().equals("jar") || project.getPackaging().equals("takari-jar")) {
        projectHelper.attachArtifact(project, provisioArchive.extension(), provisioArchive.file());
        continue;
      }
      // We have something like provisio or presto-plugin
      project.getArtifact().setFile(provisioArchive.file());
    }
  }
}
