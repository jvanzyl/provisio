package io.provis.assembly;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioModel;
import io.provis.provision.action.fileset.MakeExecutableAction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TeslaAssemblyTest extends AssemblyTestCase {
  
  @Override
  protected String name() {
    return "tesla";
  }

  @Override
  protected Map<String, String> versionMap() {
    Map<String,String> versionMap = new HashMap<String,String>();
    versionMap.put("io.tesla.maven:tesla-launch", "3.1-SNAPSHOT");
    versionMap.put("io.tesla.maven:tesla-configuration", "3.1-SNAPSHOT");
    return versionMap;
  }
  
  @Override
  protected void validateAssembly(ProvisioModel model) {
    
    
    //
    // bin
    //
    ArtifactSet bin = model.artifactSet("bin");    
    MakeExecutableAction me = (MakeExecutableAction) bin.action("executable");
    assertNotNull(me);
    assertEquals("**/mvn*", me.getIncludes());
    assertEquals("**/*.bat", me.getExcludes());

    //
    // lib
    //
    ArtifactSet lib = model.artifactSet("lib");
    assertNotNull(lib);
    assertNotNull(lib.artifact("ch.qos.logback:logback-core:1.0.7"));
    assertNotNull(lib.artifact("ch.qos.logback:logback-classic:1.0.7"));
    assertNotNull(lib.artifact("org.eclipse.aether:aether-connector-file:0.9.0.M2"));
    assertNotNull(lib.artifact("io.tesla.aether:aether-connector-okhttp:0.0.5"));
  }

  @Override
  protected void validateRuntime() {
    // Artifacts that should be here
    assertFilePresent("bin/mvn");
    assertFilePresent("lib/logback-core-1.0.7.jar");
    // Artifacts that should not
    // assertFileNotPresent("lib/");
  }

  protected void assertFilePresent(String file) {
    assertTrue(new File(outputDirectory, file).exists());
  }

  protected void assertFileNotPresent(String file) {
    assertFalse(new File(outputDirectory, file).exists());
  }

}
