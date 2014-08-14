package io.tesla.maven.plugins.provisio;

import io.provis.model.ActionDescriptor;
import io.provis.model.ProvisioningAction;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Mojo(name = "provision", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ProvisioMojo extends AbstractMojo {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject
  private MavenProjectHelper projectHelper;

  @Inject
  private RepositorySystem repositorySystem;

  @Inject
  private Map<String, ProvisioningAction> actions;

  @Parameter(defaultValue = "${project}")
  @Incremental(configuration = Configuration.ignore)
  protected MavenProject project;

  @Parameter(defaultValue = "${project.dependencyManagement}")
  @Incremental(configuration = Configuration.ignore)
  private DependencyManagement dependencyManagement;

  @Parameter(defaultValue = "${project.build.directory}/${project.artifactId}-${project.version}")
  private File outputDirectory;

  @Parameter(required = true, defaultValue = "${basedir}/src/main/provisio")
  private File descriptorDirectory;

  @Parameter(defaultValue = "${basedir}/src/main/provisio/runtime.provisio")
  private File runtimeDescriptor;

  @Parameter(defaultValue = "${repositorySystemSession}")
  private RepositorySystemSession repositorySystemSession;

  /**  @parameter expression=${archive}" */
  private File archive;

  public void execute() throws MojoExecutionException, MojoFailureException {
    RuntimeReader parser = new RuntimeReader(actionDescriptors());
    MavenProvisioner provisioner = new DefaultMavenProvisioner(repositorySystem, repositorySystemSession, project.getRemoteProjectRepositories());
    Runtime runtime;
    try {
      runtime = parser.read(new FileInputStream(runtimeDescriptor), (Map) project.getProperties());
    } catch (Exception e) {
      throw new MojoFailureException("Cannot read assembly descriptor file " + runtimeDescriptor, e);
    }
    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(outputDirectory);
    request.setModel(runtime);
    request.setVariables((Map) project.getProperties());
    ProvisioningResult result = provisioner.provision(request);
    // So the distribution is made now but it's in the descriptor so we need a good way to know. We need to augment the result with
    // archives that are created.
    projectHelper.attachArtifact(project, "tar.gz", archive);
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
}
