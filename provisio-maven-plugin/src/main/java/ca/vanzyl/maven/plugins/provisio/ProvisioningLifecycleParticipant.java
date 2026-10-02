/*
 * Copyright (C) 2015-2024 Jason van Zyl
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

import ca.vanzyl.provisio.model.Runtime;
import java.io.File;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

@Singleton
@Named("ProvisioningLifecycleParticipant")
public class ProvisioningLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    private static final String DEFAULT_DESCRIPTOR_DIRECTORY = "src/main/provisio";
    private static final String DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT = "descriptorDirectory";

    private final Provisio provisio;

    @Inject
    public ProvisioningLifecycleParticipant(Provisio provisio) {
        this.provisio = provisio;
    }

    protected String getPluginId() {
        return "provisio-maven-plugin";
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        Map<String, MavenProject> projectMap = new HashMap<String, MavenProject>();
        for (MavenProject project : session.getProjects()) {
            projectMap.put(project.getGroupId() + ":" + project.getArtifactId(), project);
        }
        for (MavenProject project : session.getProjects()) {
            for (Plugin plugin : project.getBuild().getPlugins()) {
                if (plugin.getArtifactId().equals(getPluginId())) {
                    Set<String> dependenciesInGAForm = gleanDependenciesFromExternalResource(session, project, plugin);
                    if (dependenciesInGAForm != null) {
                        //
                        // If we see a dependency here on a project that is in the reactor then we need
                        // to add this project as a dependency so that we can ensure the reactor is
                        // calculated in the correct order.
                        //
                        for (String dependencyInGAForm : dependenciesInGAForm) {
                            if (projectMap.containsKey(dependencyInGAForm)) {
                                project.getDependencies().add(buildOrderDependency(projectMap.get(dependencyInGAForm)));
                            }
                        }
                    }
                }
            }
        }
    }

    //
    // The dependency only orders the reactor: the provision mojo resolves the descriptor's artifacts itself. Maven
    // still resolves it for every mojo that requires dependency resolution, so it must resolve in any phase. A
    // packaged type such as tar.gz only exists once the dependent project has run package, which fails builds that
    // stop before package, like test-compile. Its POM always resolves from the reactor, and excluding everything
    // keeps the dependent project's own dependencies out of this project. Provided keeps it off runtime.classpath.
    //
    private static Dependency buildOrderDependency(MavenProject dependentProject) {
        Exclusion exclusion = new Exclusion();
        exclusion.setGroupId("*");
        exclusion.setArtifactId("*");

        Dependency dependency = new Dependency();
        dependency.setGroupId(dependentProject.getGroupId());
        dependency.setArtifactId(dependentProject.getArtifactId());
        dependency.setVersion(dependentProject.getVersion());
        dependency.setType("pom");
        dependency.setScope("provided");
        dependency.addExclusion(exclusion);
        return dependency;
    }

    //
    // We need to store the assembly models for each project
    //
    protected Set<String> gleanDependenciesFromExternalResource(
            MavenSession session, MavenProject project, Plugin plugin) throws MavenExecutionException {
        File descriptorDirectory;
        Xpp3Dom configuration = getMojoConfiguration(plugin);
        if (configuration != null && configuration.getChild(DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT) != null) {
            descriptorDirectory = project.getBasedir()
                    .toPath()
                    .resolve(configuration
                            .getChild(DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT)
                            .getValue())
                    .toFile();
        } else {
            descriptorDirectory = project.getBasedir()
                    .toPath()
                    .resolve(DEFAULT_DESCRIPTOR_DIRECTORY)
                    .toFile();
        }
        //
        // For all our descriptors we need to find all the artifacts requested that might refer to projects
        // in the current build so we can influence build ordering.
        //
        Set<String> dependencyCoordinatesInVersionlessForm = new HashSet<>();

        List<Runtime> runtimes = provisio.findDescriptorsInFileSystem(descriptorDirectory, project);
        for (Runtime runtime : runtimes) {
            //
            // Return all the artifacts that may have projects that contribute to the ordering of the project
            //
            dependencyCoordinatesInVersionlessForm.addAll(runtime.getGAsOfArtifacts());
        }
        return dependencyCoordinatesInVersionlessForm;
    }

    protected Xpp3Dom getMojoConfiguration(Plugin plugin) {
        //
        // We need to look in the configuration element, and then look for configuration elements
        // within the executions.
        //
        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        if (configuration == null) {
            if (!plugin.getExecutions().isEmpty()) {
                configuration = (Xpp3Dom) plugin.getExecutions().get(0).getConfiguration();
            }
        }
        return configuration;
    }
}
