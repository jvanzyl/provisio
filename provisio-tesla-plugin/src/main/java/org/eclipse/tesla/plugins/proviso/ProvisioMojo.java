package org.eclipse.tesla.plugins.proviso;

import io.provis.model.ProvisioModel;
import io.provis.parser.ProvisioModelParser;
import io.provis.provision.Provisioner;
import io.provis.provision.ProvisioningRequest;
import io.tesla.aether.TeslaAether;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
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
  @DefaultsTo("${basedir}/src/main/proviso/runtime.json")
  /**
   * @parameter expression="${runtimeDescriptor}" default-value="${basedir}/src/main/proviso/runtime.json"
   */
  private File runtimeDescriptor;  

  @Configuration
  @Required
  @DefaultsTo("${project.remoteProjectRepositories")
  /**
   * @parameter expression="${project.remoteProjectRepositories}"
   */
  private List<RemoteRepository> remoteRepositories;  
  
  public void execute() throws MojoExecutionException, MojoFailureException {
          
    StringBuffer sb = new StringBuffer();
    for(RemoteRepository r : remoteRepositories) {
      sb.append(r.getUrl()).append(",");
    }
    sb.substring(0,sb.length()-1);
    System.setProperty(TeslaAether.REMOTE_REPOSITORY, sb.toString());
    
    ProvisioModel assembly;
    
    try {
      assembly = parser.read(runtimeDescriptor, outputDirectory, getVersionMap());
    } catch (Exception e) {
      throw new MojoFailureException("Cannot read assembly descriptor file " + runtimeDescriptor, e);
    }
    
    // I need a provider for the repo system session, repository system and repositories
    
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
