package io.provis.assembly;

import io.provis.provision.VersionMapFromPom;
import io.tesla.aether.guice.maven.MavenBehaviourModule;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTestCase;

import com.google.inject.Binder;

public class VersionMapFromPomTest extends InjectedTestCase {

  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }
  
  @Inject
  private VersionMapFromPom versionMapFromPom;
  
  public void testVersionMapFromPom() throws Exception {
    Map<String,String> versionMap = versionMapFromPom.versionMap("org.apache.maven:maven:pom:3.1.1");
    for(String ga : versionMap.keySet()) {
      System.out.println(ga + " => " + versionMap.get(ga));
    }
  }
}
