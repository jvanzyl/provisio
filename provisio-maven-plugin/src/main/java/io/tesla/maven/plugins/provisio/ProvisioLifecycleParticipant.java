package io.tesla.maven.plugins.provisio;

import io.provis.model.ProvisioArtifact;
import io.provis.model.v2.ArtifactSet;
import io.provis.model.v2.Runtime;
import io.provis.model.v2.RuntimeReader;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
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

@Singleton
@Named("ProvisioLifecycleParticipant")
public class ProvisioLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  @Inject
  private RuntimeReader parser;

  protected String getPluginId() {
    return "provisio-tesla-plugin";
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
  }

  protected Set<String> process(MavenProject project, Plugin plugin) {
    Xpp3Dom configuration = getMojoConfiguration(plugin);
    File runtimeDescriptor = new File(project.getBasedir(), configuration.getChild("runtimeDescriptor").getValue());
    Runtime assembly = null;
    try {
      assembly = parser.read(new FileInputStream(runtimeDescriptor));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    Set<String> dependenciesInGAForm = new HashSet<String>();
    for (ArtifactSet fileSet : assembly.getArtifactSets()) {
      for (ProvisioArtifact gaDependency : fileSet.getArtifactMap().values()) {
        dependenciesInGAForm.add(gaDependency.getGA());
      }
      if (fileSet.getArtifactSets() != null) {
        for (ArtifactSet childFileSet : fileSet.getArtifactSets()) {
          for (ProvisioArtifact gaDependency : childFileSet.getArtifactMap().values()) {
            dependenciesInGAForm.add(gaDependency.getGA());
          }
        }
      }
    }
    return dependenciesInGAForm;
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
  //
  // DefaultMavenPluginManager.populatePluginFields, this is what we want the same behaviour from
  //
}
