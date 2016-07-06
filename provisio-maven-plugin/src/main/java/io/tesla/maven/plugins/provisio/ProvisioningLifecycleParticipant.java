/**
 * Copyright (c) 2016 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.tesla.maven.plugins.provisio;

import io.provis.model.Runtime;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.google.common.collect.Sets;

@Singleton
@Named("ProvisioningLifecycleParticipant")
public class ProvisioningLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  static final String PROVISIO_RUNTIMES = "__@provisioRuntimes";
  static final String PROVISIO_PARTICIPANT = "__@provisioParticipant";
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
                MavenProject dependentProject = projectMap.get(dependencyInGAForm);
                Dependency dependency = new Dependency();
                dependency.setGroupId(dependentProject.getGroupId());
                dependency.setArtifactId(dependentProject.getArtifactId());
                dependency.setVersion(dependentProject.getVersion());
                // It is expect that we are finding dependencies in the provisio descriptor and we want a 
                // contribution to the build order but we don't want it affecting the classpath of this
                // project. If it's not provided it's going to contribute to the runtime.classpath which
                // is not desired.
                dependency.setScope("provided");
                project.getDependencies().add(dependency);
              }
            }
          }
        }
      }
    }
  }

  //
  // We need to store the assembly models for each project
  //
  protected Set<String> gleanDependenciesFromExternalResource(MavenSession session, MavenProject project, Plugin plugin) throws MavenExecutionException {
    File descriptorDirectory;
    Xpp3Dom configuration = getMojoConfiguration(plugin);
    if (configuration != null && configuration.getChild(DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT) != null) {
      descriptorDirectory = new File(project.getBasedir(), configuration.getChild(DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT).getValue());
    } else {
      descriptorDirectory = new File(project.getBasedir(), DEFAULT_DESCRIPTOR_DIRECTORY);
    }
    //
    // For all our descriptors we need to find all the artifacts requested that might refer to projects
    // in the current build so we can influence build ordering.
    //
    Set<String> dependencyCoordinatesInVersionlessForm = Sets.newHashSet();

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
