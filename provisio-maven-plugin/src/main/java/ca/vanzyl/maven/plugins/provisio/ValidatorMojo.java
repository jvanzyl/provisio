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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mojo(name = "validateDependencies", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class ValidatorMojo
        extends BaseMojo
{
    /**
     * Location of an existing POM file in which dependencies should be validated.
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
        Set<String> dependencies = flattenDependencies(getDependencies(artifacts));
        Set<String> modelDependencies = flattenDependencies(model.getDependencies()
                .stream()
                .filter(d -> d.getScope() == null || d.getScope().equals("compile"))
                .collect(Collectors.toList()));

        checkDependencies(modelDependencies, dependencies);
    }

    private Set<String> flattenDependencies(List<Dependency> dependencies)
    {
        return dependencies
                .stream()
                .map(d -> d.getGroupId() +
                        ":" + d.getArtifactId() +
                        prefixed(d.getType(), ":jar") +
                        prefixed(d.getClassifier()) +
                        ":" + d.getVersion())
                .collect(Collectors.toSet());
    }

    private String prefixed(String value)
    {
        return prefixed(value, "");
    }

    private String prefixed(String value, String defaultValue)
    {
        if (value == null) {
            return defaultValue;
        }
        return ":" + value;
    }

    /**
     * Throws an exception if there are no extra or missing dependencies in the model.
     */
    private void checkDependencies(Set<String> actual, Set<String> expected)
            throws MojoFailureException
    {
        HashSet<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);

        actual.removeAll(expected);

        if (!missing.isEmpty()) {
            throw new MojoFailureException("Missing dependencies: " + String.join(", ", missing));
        }

        if (!actual.isEmpty()) {
            throw new MojoFailureException("Extra dependencies: " + String.join(", ", actual));
        }
    }
}
