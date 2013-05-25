package org.eclipse.tesla.plugins.proviso;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioModel;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;


@Singleton
@Named("ProvisoLifecycleParticipant")
public class ProvisoLifecycleParticipant extends DependencyContributingLifecycleParticipant {

  private Marshaller<ProvisioModel> marshaller;

  public ProvisoLifecycleParticipant() {
    marshaller = new XmlMarshaller<ProvisioModel>();
  }

  @Override
  protected String getPluginId() {
    return "tesla-proviso-plugin";
  }

  @Override
  protected String getMojoConfigurationElementName() {
    return "assembly";
  }

  @Override
  protected Set<String> process(MavenProject project, Plugin plugin) {
    return null;
  }

  /*
  @Override
  protected Set<String> process(MavenProject project, Plugin plugin) {
    Xpp3Dom configuration = getMojoConfiguration(plugin);
    ProvisioModel assembly = new ProvisioModel();
    marshaller.unmarshall(assembly, configuration);
    Set<String> dependenciesInGAForm = new HashSet<String>();
    for (ArtifactSet fileSet : assembly.getFileSets()) {
      for (ProvisoArtifact gaDependency : fileSet.getArtifactMapKeyedByGA().values()) {
        dependenciesInGAForm.add(gaDependency.getGA());
      }
      for (ArtifactSet childFileSet : fileSet.getFileSets()) {
        for (ProvisoArtifact gaDependency : childFileSet.getArtifactMapKeyedByGA().values()) {
          dependenciesInGAForm.add(gaDependency.getGA());
        }
      }
    }
    return dependenciesInGAForm;
  }
  */
  
  //
  // DefaultMavenPluginManager.populatePluginFields, this is what we want the same behaviour from
  //
}
