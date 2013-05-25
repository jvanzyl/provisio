package io.provis.assembly;

import io.provis.provision.VersionMapFromPom;
import io.tesla.aether.guice.maven.MavenBehaviourModule;

import java.util.Map;

import javax.inject.Inject;

import org.eclipse.sisu.containers.InjectedTestCase;

import com.google.inject.Binder;

public class VersionMapFromPomTest extends InjectedTestCase {

  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }
  
  @Inject
  private VersionMapFromPom versionMapFromPom;
  
  public void testVersionMapFromPom() throws Exception {
    Map<String,String> versionMap = versionMapFromPom.versionMap("io.tesla.maven:maven:3.1.0");
    for(String ga : versionMap.keySet()) {
      System.out.println(ga + " => " + versionMap.get(ga));
    }
  }
}
