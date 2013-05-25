package io.provis.provision;

import java.util.HashMap;
import java.util.Map;

import io.tesla.aether.TeslaAether;

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
  
  //
  // We take the coordinate for an artifact and find the POM for it. So we take:
  // 
  // groupId:artifactId:version
  //
  // change it to 
  //
  // groupId:artifactId:pom:version
  //
  public Map<String,String> versionMap(String coordinate) throws ArtifactResolutionException, ModelBuildingException {
    Map<String,String> versionMap = new HashMap<String,String>();
    int i = coordinate.lastIndexOf(":");
    String pomCoordinate = coordinate.substring(0, i) + ":pom:" + coordinate.substring(i);
    Model model = aether.resolveModel(pomCoordinate);
    for(Dependency d : model.getDependencyManagement().getDependencies()) {
      versionMap.put(d.getGroupId() + ":" + d.getArtifactId(), d.getVersion());
    }
    return versionMap;
  }
}
