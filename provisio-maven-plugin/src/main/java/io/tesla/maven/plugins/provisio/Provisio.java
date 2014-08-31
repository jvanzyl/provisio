package io.tesla.maven.plugins.provisio;

import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.Actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Named
@Singleton
public class Provisio {

  private Logger logger = LoggerFactory.getLogger(Provisio.class);

  private final ArtifactHandlerManager artifactHandlerManager;

  @Inject
  public Provisio(ArtifactHandlerManager artifactHandlerManager) {
    this.artifactHandlerManager = artifactHandlerManager;
  }

  public List<Runtime> findDescriptors(File descriptorDirectory, MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    runtimes.addAll(findDescriptorsInFileSystem(descriptorDirectory, project));
    runtimes.addAll(findDescriptorsInClasspath(project));
    return runtimes;
  }

  public List<Runtime> findDescriptorsInFileSystem(File descriptorDirectory, MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    if (descriptorDirectory.exists()) {
      try {
        List<File> descriptors = FileUtils.getFiles(descriptorDirectory, "*.xml", null);
        for (File descriptor : descriptors) {
          Runtime runtime = parseDescriptor(new FileInputStream(descriptor), project);
          runtimes.add(runtime);
        }
      } catch (IOException e) {
        throw new RuntimeException(String.format("Error parsing provisioning descriptors in %s.", descriptorDirectory), e);
      }
    }
    return runtimes;
  }

  public List<Runtime> findDescriptorsInClasspath(MavenProject project) {
    List<Runtime> runtimes = Lists.newArrayList();
    Collection<ClassRealm> extensionRealms = project.getClassRealm().getImportRealms();
    if (extensionRealms != null) {
      for (ClassRealm extensionRealm : extensionRealms) {
        String[] s = StringUtils.split(extensionRealm.getId(), ":");
        String extensionArtifactId = s[1];
        String extensionVersion = s[2];
        // presto-maven-plugin --> presto-plugin
        if (extensionArtifactId.replace("-maven", "").equals(project.getPackaging())) {
          String descriptorResourceLocation = String.format("META-INF/provisio/%s.xml", project.getPackaging());
          logger.info(String.format("Looking for descriptor %s in %s version %s", descriptorResourceLocation, extensionArtifactId, extensionVersion));
          try {
            Enumeration<URL> descriptors = extensionRealm.getResources(descriptorResourceLocation);
            for (URL descriptorUrl : Collections.list(descriptors)) {
              InputStream inputStream = descriptorUrl.openStream();
              if (inputStream != null) {
                Runtime runtime = parseDescriptor(inputStream, project);
                runtimes.add(runtime);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(String.format("Error parsing provisioning descriptors from %s.", descriptorResourceLocation), e);
          }
        }
      }
    }
    return runtimes;
  }

  public Runtime parseDescriptor(InputStream inputStream, MavenProject project) {
    RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), versionMap(project));
    Map<String, String> variables = Maps.newHashMap();
    variables.putAll((Map) project.getProperties());
    variables.put("project.version", project.getVersion());
    variables.put("project.groupId", project.getArtifactId());
    variables.put("project.artifactId", project.getArtifactId());
    variables.put("basedir", project.getBasedir().getAbsolutePath());
    return parser.read(inputStream, variables);
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

  public List<String> getManagedDependencies(MavenProject project) {
    List<String> managedDependencies = Lists.newArrayList();
    if (!project.getDependencyManagement().getDependencies().isEmpty()) {
      for (Dependency managedDependency : project.getDependencyManagement().getDependencies()) {
        managedDependencies.add(toCoordinate(managedDependency));
      }
    }
    return managedDependencies;
  }

  public String toCoordinate(Dependency d) {
    StringBuffer sb = new StringBuffer().append(d.getGroupId()).append(":").append(d.getArtifactId()).append(":").append(d.getType());
    if (d.getClassifier() != null && d.getClassifier().isEmpty() == false) {
      sb.append(":").append(d.getClassifier());
    }
    sb.append(":").append(d.getVersion());
    return sb.toString();
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
