package io.tesla.maven.plugins.provisio;

import io.provis.model.ProvisioModel;
import io.provis.parser.ProvisioModelParser;
import io.provis.provision.Provisioner;
import io.provis.provision.ProvisioningRequest;
import io.tesla.aether.TeslaAether;
import io.tesla.aether.internal.DefaultTeslaAether;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.sonatype.maven.plugin.Configuration;
import org.sonatype.maven.plugin.DefaultPhase;
import org.sonatype.maven.plugin.DefaultsTo;
import org.sonatype.maven.plugin.Goal;
import org.sonatype.maven.plugin.LifecyclePhase;
import org.sonatype.maven.plugin.Required;
import org.sonatype.maven.plugin.RequiresDependencyResolution;

@Goal("provision")
@DefaultPhase(LifecyclePhase.PACKAGE)
@RequiresDependencyResolution
/**
 * @goal provision
 * @phase package
 * @author Jason van Zyl
 */
public class ProvisioMojo extends AbstractMojo {

  @Inject
  private Logger logger;

  @Inject
  private Provisioner provisioner;

  @Inject
  private ProvisioModelParser parser;

  @Configuration
  @DefaultsTo("${project}")
  /**
   * @parameter expression="${project}"
   */
  private MavenProject project;
  
  
  @Configuration
  @DefaultsTo("${project.dependencyManagement}")
  /**
   * @parameter expression="${project.dependencyManagement}"
   */
  private DependencyManagement dependencyManagement;

  @Configuration
  @DefaultsTo("${project.build.directory}/proviso/runtime")
  /**
   * @parameter expression="${outputDirectory}" default-value="${project.build.directory}/${project.artifactId}-${project.version}"
   */
  private File outputDirectory;

  @Configuration
  @Required
  @DefaultsTo("${basedir}/src/main/provisio")
  /**
   * @parameter expression="${descriptorDirectory}" default-value="${basedir}/src/main/provisio"
   */
  private File descriptorDirectory;

  /**
   * @parameter expression="${runtimeDescriptor}" default-value="${basedir}/src/main/provisio/runtime.provisio"
   */
  private File runtimeDescriptor;

  @Configuration
  @Required
  @DefaultsTo("${project.remoteProjectRepositories")
  /**
   * @parameter expression="${project.remoteProjectRepositories}"
   */
  private List<RemoteRepository> remoteRepositories;

  /**
   * The current repository/network configuration of Maven.
   * 
   * @parameter default-value="${repositorySystemSession}"
   * @readonly
   */
  private RepositorySystemSession repositorySystemSession;

  public void execute() throws MojoExecutionException, MojoFailureException {

    TeslaAether aether = new DefaultTeslaAether(remoteRepositories, repositorySystemSession);
    provisioner.setAether(aether);

    //    List<File> descriptors;
    //    try {
    //      descriptors = FileUtils.getFiles(descriptorDirectory, "**/*.provisio", null);
    //    } catch (IOException e) {
    //      throw new MojoExecutionException("Failed to find to provisio descriptors.", e);
    //    }
    //
    //    for (File descriptor : descriptors) {
    //
    //      ProvisioModel assembly;
    //      try {
    //        assembly = parser.read(descriptor, outputDirectory, new HashMap<String, String>());
    //      } catch (Exception e) {
    //        throw new MojoFailureException("Cannot read assembly descriptor file " + descriptor, e);
    //      }
    //
    //      ProvisioningRequest request = new ProvisioningRequest();
    //      request.setOutputDirectory(outputDirectory);
    //      request.setRuntimeAssembly(assembly);
    //      provisioner.provision(request);
    //    }

    ProvisioModel assembly;
    try {
      System.out.println(">> " + project.getProperties());
      assembly = parser.read(runtimeDescriptor, outputDirectory, (Map)project.getProperties());
    } catch (Exception e) {
      throw new MojoFailureException("Cannot read assembly descriptor file " + runtimeDescriptor, e);
    }

    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(outputDirectory);
    request.setRuntimeAssembly(assembly);
    provisioner.provision(request);

  }

  // I don't really need this in a non-reactor build. In a separate assembly project I would pull the version map from another source like a POM
  private Map<String, String> getVersionMap() {
    Map<String, String> versionMap = new HashMap<String, String>();
    if (dependencyManagement.getDependencies().isEmpty() == false) {
      for (Dependency managedDependency : dependencyManagement.getDependencies()) {
        String ga = managedDependency.getGroupId() + ":" + managedDependency.getArtifactId();
        if (getLog().isDebugEnabled()) {
          getLog().debug("Adding " + ga + " to dependencyVersionMap ==> ");
        }
        versionMap.put(ga, managedDependency.getVersion());
      }
    }
    return versionMap;
  }
}
