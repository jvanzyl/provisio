package io.tesla.maven.plugins.provisio;

import io.provis.model.ActionDescriptor;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.DefaultMavenProvisioner;
import io.provis.provision.MavenProvisioner;
import io.provis.provision.ProvisioningRequest;
import io.provis.provision.ProvisioningResult;
import io.provis.provision.action.artifact.UnpackAction;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Mojo(name = "provision", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ProvisioningMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  private MavenProjectHelper projectHelper;

  @Inject
  private RepositorySystem repositorySystem;

  @Inject
  private ArtifactHandlerManager artifactHandlerManager;
  
  @Parameter(defaultValue = "${project}")
  @Incremental(configuration = Configuration.ignore)
  protected MavenProject project;

  @Parameter(defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repositorySystemSession;

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File outputDirectory;

  @Parameter(required = true, defaultValue = "${basedir}/src/main/provisio")
  private File descriptorDirectory;

  public void execute() throws MojoExecutionException, MojoFailureException {
    List<File> descriptors = findDescriptors();
    for (File descriptor : descriptors) {
      RuntimeReader parser = new RuntimeReader(actionDescriptors(), versionMap(project));
      MavenProvisioner provisioner = new DefaultMavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());
      Map<String,String> variables = Maps.newHashMap();
      variables.putAll((Map) project.getProperties());
      variables.put("project.version", project.getVersion());
      variables.put("project.groupId", project.getArtifactId());
      variables.put("project.artifactId", project.getArtifactId());
      
      Runtime runtime;
      try {
        runtime = parser.read(new FileInputStream(descriptor), variables);
      } catch (Exception e) {
        throw new MojoFailureException("Cannot read assembly descriptor file " + descriptor, e);
      }
      ProvisioningRequest request = new ProvisioningRequest();
      request.setOutputDirectory(outputDirectory);
      request.setModel(runtime);
      request.setVariables(variables);
      ProvisioningResult result = provisioner.provision(request);
    }
  }

  private List<File> findDescriptors() {
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

  private List<ActionDescriptor> actionDescriptors() {
    List<ActionDescriptor> actionDescriptors = Lists.newArrayList();
    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "unpack";
      }

      @Override
      public Class<?> getImplementation() {
        return UnpackAction.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
            "filter", "includes", "excludes", "flatten", "useRoot"
        };
      }
    });
    return actionDescriptors;
  }

  //
  // The version map to use when versions are not specified for the artifacts in the assembly/runtime document.
  //
  private Map<String, String> versionMap(MavenProject project) {
    Map<String, String> versionMap = Maps.newHashMap();
    if (!project.getDependencyManagement().getDependencies().isEmpty()) {
      for (Dependency managedDependency : project.getDependencyManagement().getDependencies()) {
        String versionlessCoordinate = toVersionlessCoordinate(managedDependency);
        getLog().debug("Adding " + versionlessCoordinate + " to dependencyVersionMap ==> " + managedDependency.getVersion());
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
