/**
 * Copyright (C) 2015-2020 Jason van Zyl
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.maven.plugins.provisio;

import ca.vanzyl.provisio.MavenProvisioner;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.Runtime;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "generateDependencies", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GeneratorMojo
        extends BaseMojo
{
    /**
     * Where to put the dependency extended pom.
     */
    @Parameter(property = "dependencyExtendedPomLocation", defaultValue = "${project.build.directory}/generated/provisio/dependency-extended-pom.xml")
    private File dependencyExtendedPomLocation;

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException
    {
        MavenProvisioner provisioner = new MavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());

        List<ProvisioArtifact> artifacts = new ArrayList<>();
        for (Runtime runtime : provisio.findDescriptors(descriptorDirectory, project)) {
            runtime.addArtifactSetReference("runtime.classpath", getRuntimeClasspathAsArtifactSet());

            ProvisioningRequest request = getRequest(runtime);
            try {
                artifacts.addAll(provisioner.getArtifacts(request));
            }
            catch (Exception e) {
                throw new MojoExecutionException("Error resolving artifacts.", e);
            }
        }
        if (artifacts.isEmpty()) {
            return;
        }
        checkDuplicates(artifacts);
        Model model = project.getOriginalModel().clone();
        List<Dependency> dependencies = getDependencies(artifacts);
        mergeDependencies(model, dependencies);
        writeModel(model);
        project.setPomFile(dependencyExtendedPomLocation);
    }

    private void writeModel(Model model)
            throws MojoExecutionException
    {
        getLog().info( "Dependency-extended POM written at: " + dependencyExtendedPomLocation.getAbsolutePath() );
        try {
            Files.createDirectories(dependencyExtendedPomLocation.toPath().getParent());
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating parent directories for the POM file: " + e.getMessage(), e);
        }
        try (Writer writer = WriterFactory.newXmlWriter(dependencyExtendedPomLocation)) {
            new MavenXpp3Writer().write(writer, model);
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error writing POM file: " + e.getMessage(), e);
        }
    }
}
