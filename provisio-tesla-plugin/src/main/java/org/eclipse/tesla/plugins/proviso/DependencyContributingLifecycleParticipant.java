package org.eclipse.tesla.plugins.proviso;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public abstract class DependencyContributingLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  protected abstract String getPluginId();

  protected String getMojoConfigurationElementName() {
    return null;
  }

  protected abstract Set<String> process(MavenProject project, Plugin plugin);

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {

    Map<String, MavenProject> projectMap = new HashMap<String, MavenProject>();
    for (MavenProject project : session.getProjects()) {
      projectMap.put(project.getGroupId() + ":" + project.getArtifactId(), project);
    }

    for (MavenProject project : session.getProjects()) {
      for (Plugin plugin : project.getBuild().getPlugins()) {
        if (plugin.getArtifactId().equals(getPluginId())) {
          Set<String> dependenciesInGAForm = process(project, plugin);
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
    super.afterProjectsRead(session);
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

    //
    // We are assuming this plugin is in the form where the configuration consists of something that
    // will map to a single object.
    //
    int childCount = configuration.getChildCount();
    if (childCount == 1) {
      return configuration.getChild(0);
    } else if (getMojoConfigurationElementName() != null) {
      return configuration.getChild(getMojoConfigurationElementName());
    } else {
      throw new RuntimeException("The mojo configuration must either have a single element, or you must specify the element that is mapped.");
    }
  }
}
