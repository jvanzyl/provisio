package io.provis.assembly;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioModel;
import io.provis.provision.action.artifact.UnpackAction;
import io.provis.provision.action.fileset.MakeExecutableAction;
import io.provis.provision.action.fileset.UpdateJarAction;

import java.io.File;

public class TeslaFromMavenAssemblyTest extends AssemblyTestCase {
  
  @Override
  protected String name() {
    return "tesla-from-maven";
  }

  protected File getOutputDirectory() {
    return new File("/Users/jvanzyl/tesla-3.1.1");
  }

  protected void validateAssembly(ProvisioModel model) {
    
    // root
    ArtifactSet root = model.fileSet("root");
    ProvisioArtifact b = root.artifact("io.tesla.maven:apache-maven:tar.gz:bin:3.1.2");
    UnpackAction unpack = (UnpackAction) b.action("unpack");
    assertNotNull(unpack);
    assertFalse(unpack.isUseRoot());
    assertEquals("lib/slf4j-simple**,lib/wagon-file*,lib/wagon-http*,bin/**,conf/**", unpack.getExcludes());
    assertNotNull(unpack.getArtifact());

    //UpdateJarAction updateJar = (UpdateJarAction) root.action("updateJar");
    //assertNotNull(updateJar);
    //assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/build.properties", updateJar.getUpdates().get("org/apache/maven/messages/build.properties"));
    //assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/components.xml", updateJar.getUpdates().get("META-INF/plexus/components.xml"));
    
    //MakeExecutableAction me = (MakeExecutableAction) root.action("executable");
    //assertNotNull(me);
    //assertEquals("**/mvn*", me.getIncludes());
    //assertEquals("**/*.bat", me.getExcludes());

    // lib
    ArtifactSet lib = model.fileSet("lib");
    assertNotNull(lib);
    assertNotNull(lib.artifact("ch.qos.logback:logback-core:1.0.7"));
    assertNotNull(lib.artifact("ch.qos.logback:logback-classic:1.0.7"));
    assertNotNull(lib.artifact("org.eclipse.aether:aether-connector-file:0.9.0.M2"));
    assertNotNull(lib.artifact("io.tesla.aether:aether-connector-okhttp:0.0.4"));
  }

  @Override
  protected void validateRuntime() {
    // Artifacts that should be here
    assertFilePresent("bin/mvn");
    //assertFilePresent("lib/logback-core-1.0.7.jar");
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
