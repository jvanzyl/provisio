package io.tesla.maven.plugins.provisio;

import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.Actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Named
@Singleton
public class Provisio {

  private final ArtifactHandlerManager artifactHandlerManager;
 
  @Inject
  public Provisio(ArtifactHandlerManager artifactHandlerManager) {
    this.artifactHandlerManager = artifactHandlerManager;
  }
  
  public List<Runtime> parseDescriptors(File descriptorDirectory, MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
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
        runtimes.add(runtime);
      } catch (Exception e) {
        throw new RuntimeException(String.format("Error reading provisioning descriptor %s for project %s.", descriptor, project.getArtifactId()), e);
      }
    }    
    return runtimes;
  }
  
  private List<File> findDescriptors(File descriptorDirectory) {
    List<File> descriptors = Lists.newArrayList();
    if (descriptorDirectory.exists()) {
      try {
        return FileUtils.getFiles(descriptorDirectory, "*.xml", null);
      } catch (IOException e) {
        throw new RuntimeException(String.format("Error finding provisioning descriptors in %s.", descriptorDirectory), e);
      }
    }
    return descriptors;
  }

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
