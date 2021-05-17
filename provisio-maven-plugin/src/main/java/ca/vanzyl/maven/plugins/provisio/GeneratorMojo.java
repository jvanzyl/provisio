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
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mojo(name = "generateDependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GeneratorMojo
        extends BaseMojo
{
    /**
     * Location of an existing POM file in which dependencies should be updated.
     */
    @Parameter(required = true, property = "pomFile", defaultValue = "${basedir}/pom.xml")
    private File pomFile;

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
        checkDuplicates(artifacts);
        Model model = readModel(pomFile, project.getModel().clone());
        List<Dependency> dependencies = getDependencies(artifacts);
        mergeDependencies(model, dependencies);
        writeModel(pomFile, model);
    }

    private void mergeDependencies(Model model, List<Dependency> dependencies)
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
                        .thenComparing(Dependency::getVersion)
                        .thenComparing(Dependency::getClassifier, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Dependency::getType, Comparator.nullsFirst(Comparator.naturalOrder())));
        model.setDependencies(sorted);
    }

    private void writeModel(File pomFile, Model model)
            throws MojoExecutionException
    {
        Writer writer = null;
        try {
            writer = WriterFactory.newXmlWriter(pomFile);
            new MavenXpp3Writer().write(writer, model);
            writer.close();
        }
        catch (IOException e) {
            throw new MojoExecutionException("Error writing POM file: " + e.getMessage(), e);
        }
        finally {
            IOUtil.close(writer);
        }
    }
}
