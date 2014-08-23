package io.tesla.maven.plugins.provisio;

import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.Actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Singleton
@Named("ProvisioningLifecycleParticipant")
public class ProvisioningLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private final Map<String,Runtime> runtimes;
  private final ArtifactHandlerManager artifactHandlerManager;
  
  @Inject
  public ProvisioningLifecycleParticipant(ArtifactHandlerManager artifactHandlerManager) {
    this.artifactHandlerManager = artifactHandlerManager;
    this.runtimes = Maps.newHashMap();
  }
  
  private static final String DEFAULT_DESCRIPTOR_DIRECTORY = "src/main/provisio";
  private static final String DESCRIPTOR_DIRECTORY_CONFIG_ELEMENT = "descriptorDirectory";

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
    List<File> descriptors = findDescriptors(descriptorDirectory);
    for (File descriptor : descriptors) {
      try {
        RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), versionMap(project));
        Map<String, String> variables = Maps.newHashMap();
        variables.putAll((Map) project.getProperties());
        variables.put("project.version", project.getVersion());
        variables.put("project.groupId", project.getArtifactId());
        variables.put("project.artifactId", project.getArtifactId());
        variables.put("basedir", project.getBasedir().getAbsolutePath());        
        Runtime runtime = parser.read(new FileInputStream(descriptor), variables);
        //
        // Return all the artifacts that may have projects that contribute to the ordering of the project
        // 
        dependencyCoordinatesInVersionlessForm.addAll(runtime.getVersionlessCoordinatesOfArtifacts());
        //
        // Add
        //
      } catch (Exception e) {
        throw new MavenExecutionException(String.format("Error reading provisioning descriptor %s for project %s.", descriptor, project.getArtifactId()), e);
      }
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
      configuration = (Xpp3Dom) plugin.getExecutions().get(0).getConfiguration();
    }
    return configuration;
  }

  private List<File> findDescriptors(File descriptorDirectory) {
    List<File> descriptors = Lists.newArrayList();
    if (descriptorDirectory.exists()) {
      try {
        return FileUtils.getFiles(descriptorDirectory, "*.xml", null);
      } catch (IOException e) {
        // ignore
      }
    }
    return descriptors;
  }
  
  //
  //
  //
  
  //
  // The version map to use when versions are not specified for the artifacts in the assembly/runtime document.
  //
  private Map<String, String> versionMap(MavenProject project) {
    Map<String, String> versionMap = Maps.newHashMap();
    if (!project.getDependencyManagement().getDependencies().isEmpty()) {
      for (Dependency managedDependency : project.getDependencyManagement().getDependencies()) {
        String versionlessCoordinate = toVersionlessCoordinate(managedDependency);
        versionMap.put(versionlessCoordinate, managedDependency.getVersion());
      }
    }
    //
    // Add a map entry for the project itself in the event that its not in dependency management so that users
    // don't have to put versions in the descriptor when including the project being built.
    //
    versionMap.put(toVersionlessCoordinate(project), project.getVersion());

    return versionMap;
  }

  public String toVersionlessCoordinate(Dependency d) {
    StringBuffer sb = new StringBuffer().append(d.getGroupId()).append(":").append(d.getArtifactId()).append(":").append(d.getType());
    if (d.getClassifier() != null && d.getClassifier().isEmpty() == false) {
      sb.append(":").append(d.getClassifier());
    }
    return sb.toString();
  }

  public String toVersionlessCoordinate(MavenProject project) {
    String extension = artifactHandlerManager.getArtifactHandler(project.getPackaging()).getExtension();
    StringBuffer sb = new StringBuffer().append(project.getGroupId()).append(":").append(project.getArtifactId()).append(":").append(extension);
    return sb.toString();
  }
  
}
