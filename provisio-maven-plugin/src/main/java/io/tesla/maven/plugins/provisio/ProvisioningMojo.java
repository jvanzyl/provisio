package io.tesla.maven.plugins.provisio;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.Actions;
import io.provis.provision.DefaultMavenProvisioner;
import io.provis.provision.MavenProvisioner;
import io.takari.incrementalbuild.Incremental;
import io.takari.incrementalbuild.Incremental.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.artifact.handler.ArtifactHandler;
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
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.artifact.DefaultArtifactType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

@Mojo(name = "provision", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ProvisioningMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

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
      RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), versionMap(project));
      MavenProvisioner provisioner = new DefaultMavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());
      Map<String, String> variables = Maps.newHashMap();
      variables.putAll((Map) project.getProperties());
      variables.put("project.version", project.getVersion());
      variables.put("project.groupId", project.getArtifactId());
      variables.put("project.artifactId", project.getArtifactId());
      variables.put("basedir", project.getBasedir().getAbsolutePath());

      Runtime runtime;
      try {
        runtime = parser.read(new FileInputStream(descriptor), variables);
      } catch (Exception e) {
        throw new MojoFailureException("Cannot read assembly descriptor file " + descriptor, e);
      }
      //
      // Add the ArtifactSet reference for the runtime classpath
      //
      runtime.addArtifactSetReference("runtime.classpath", getRuntimeClasspathAsArtifactSet());
      //
      // Provision the runtime
      //
      ProvisioningRequest request = new ProvisioningRequest();
      request.setOutputDirectory(outputDirectory);
      request.setModel(runtime);
      request.setVariables(variables);
      ProvisioningResult result = provisioner.provision(request);

      //
      // We need to distinguish between a mode of building a single project and producing a set of distributions
      //
      //for (File archives : result.getArchives()) {
      //}

      if (result.getArchives() != null) {
        if (result.getArchives().size() == 1) {
          File file = result.getArchives().get(0);
          project.getArtifact().setFile(file);
        }
      }
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

  //
  // We want to produce an artifact set the corresponds to the runtime classpath of the project. This ArtifactSet will contain:
  //
  // - runtime dependencies
  // - any artifacts produced by this build
  //  
  public ArtifactSet getRuntimeClasspathAsArtifactSet() {
    //
    // project.getArtifacts() will return us the runtime artifacts as that's the resolution scope we have requested
    // for this Mojo. I think this will be sufficient for anything related to creating a runtime.
    //
    ArtifactSet artifactSet = new ArtifactSet();
    for (org.apache.maven.artifact.Artifact mavenArtifact : project.getArtifacts()) {
      artifactSet.addArtifact(new ProvisioArtifact(toArtifact(mavenArtifact)));
    }
    //
    // We also need to definitively know what others types of runtime artifacts have been created. Right now there
    // is no real protocol for knowing what something like, say, the JAR plugin did to drop off a file somewhere. We
    // need to improve this but for now we'll look.
    //
    File jar = new File(project.getBuild().getDirectory(), project.getArtifactId() + "-" + project.getVersion() + ".jar");
    if(jar.exists()) {
      ProvisioArtifact jarArtifact = new ProvisioArtifact(project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());
      jarArtifact.setFile(jar);
      artifactSet.addArtifact(jarArtifact);
    }
    
    return artifactSet;
  }

  private static Artifact toArtifact(org.apache.maven.artifact.Artifact artifact) {
    if (artifact == null) {
      return null;
    }

    String version = artifact.getVersion();
    if (version == null && artifact.getVersionRange() != null) {
      version = artifact.getVersionRange().toString();
    }

    Map<String, String> props = null;
    if (org.apache.maven.artifact.Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) {
      String localPath = (artifact.getFile() != null) ? artifact.getFile().getPath() : "";
      props = Collections.singletonMap(ArtifactProperties.LOCAL_PATH, localPath);
    }

    Artifact result = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getArtifactHandler().getExtension(), version, props, newArtifactType(
        artifact.getType(), artifact.getArtifactHandler()));
    result = result.setFile(artifact.getFile());

    return result;
  }

  private static ArtifactType newArtifactType(String id, ArtifactHandler handler) {
    return new DefaultArtifactType(id, handler.getExtension(), handler.getClassifier(), handler.getLanguage(), handler.isAddedToClasspath(), handler.isIncludesDependencies());
  }

}
