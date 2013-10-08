package io.provis.provision;

import io.tesla.aether.TeslaAether;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.eclipse.aether.resolution.ArtifactResolutionException;

@Named
@Singleton
public class VersionMapFromPom {

  private TeslaAether aether;
  
  @Inject
  public VersionMapFromPom(TeslaAether aether) {
    this.aether = aether;
  }
  
  public Map<String,String> versionMap(String pomCoordinate) throws ArtifactResolutionException, ModelBuildingException {
    Map<String,String> versionMap = new HashMap<String,String>();
    Model model = aether.resolveModel(pomCoordinate);
    for(Dependency d : model.getDependencyManagement().getDependencies()) {
      versionMap.put(d.getGroupId() + ":" + d.getArtifactId(), d.getVersion());
    }
    return versionMap;
  }
}
