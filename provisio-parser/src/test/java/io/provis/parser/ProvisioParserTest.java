package io.provis.parser;

import io.provis.action.MakeExecutable;
import io.provis.action.TestAction0;
import io.provis.action.Unpack;
import io.provis.action.UpdateJar;
import io.provis.action.Validate;
import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.v2.ProvisioModel;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.sisu.launch.InjectedTestCase;

public class ProvisioParserTest extends InjectedTestCase {

  @Inject
  private ProvisioModelParser parser;

  public void testProvisioModelParser() throws Exception {

    ProvisioModel m = model("test");
    assertNotNull("The model should not be null.", m);
    assertEquals("tesla", m.getId());

    // bin
    ArtifactSet bin = m.artifactSet("bin");
    assertNotNull(bin);
    ProvisioArtifact artifact0 = bin.artifact("g0:a0:1.0");
    assertNotNull(artifact0);
    TestAction0 action0 = (TestAction0) artifact0.action("testAction0");
    assertNotNull(action0);
    assertEquals("bar", action0.getScalar());
    assertEquals("foo", action0.getLiteral());
    assertEquals("one", action0.getList().get(0));
    assertEquals("two", action0.getList().get(1));
    assertEquals("three", action0.getList().get(2));
    assertArtifact(bin.artifact("g1:a1:1.0"), "g1:a1:1.0");

    // root
    ArtifactSet root = m.artifactSet("root");
    ProvisioArtifact b = root.artifact("org.apache.maven:apache-maven:tar.gz:bin:3.1-SNAPSHOT");
    Unpack unpack = (Unpack) b.action("unpack");
    assertNotNull(unpack);
    assertFalse(unpack.isUseRoot());
        
    UpdateJar updateJar = (UpdateJar) root.action("updateJar");
    assertNotNull(updateJar);
    assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/build.properties", updateJar.getUpdates().get("org/apache/maven/messages/build.properties"));
    assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/components.xml", updateJar.getUpdates().get("META-INF/plexus/components.xml"));
    
    assertNotNull(root.action("makeExecutable"));
    MakeExecutable me = (MakeExecutable) root.action("makeExecutable");
    assertNotNull(me);
    assertEquals("**/mvn*", me.getIncludes().get(0));
    assertEquals("**/*.bat", me.getExcludes().get(0));

    // lib
    ArtifactSet lib = m.artifactSet("lib");
    assertNotNull(lib);
    assertNotNull(lib.artifact("ch.qos.logback:logback-core:1.0.7"));
    assertNotNull(lib.artifact("ch.qos.logback:logback-classic:1.0.7"));
    assertNotNull(lib.artifact("org.eclipse.aether:aether-connector-file:0.9.0.M1"));
    assertNotNull(lib.artifact("io.tesla.aether:aether-connector-okhttp:0.0.1-SNAPSHOT"));    
    
    Validate validate = (Validate) m.action("validate");
    assertNotNull(validate);
    assertTrue(validate.isValidate());
  }

  public void testProvisioModelParserWhereThereAreNoVersions() throws Exception {

    Map<String,String> versionMap = new HashMap<String,String>();
    versionMap.put("groupId:artifactId", "5.0.0");
    ProvisioModel m = model("test-without-versions", versionMap);
    assertNotNull("The model should not be null.", m);
    assertEquals("tesla", m.getId());

    // etc
    ArtifactSet etc = m.artifactSet("etc");
    ProvisioArtifact noVersion = etc.artifact("groupId:artifactId:5.0.0");
    assertNotNull(noVersion);
    
    // bin
    ArtifactSet bin = m.artifactSet("bin");
    assertNotNull(bin);
    ProvisioArtifact artifact0 = bin.artifact("g0:a0:1.0");
    assertNotNull(artifact0);
    TestAction0 action0 = (TestAction0) artifact0.action("testAction0");
    assertNotNull(action0);
    assertEquals("bar", action0.getScalar());
    assertEquals("foo", action0.getLiteral());
    assertEquals("one", action0.getList().get(0));
    assertEquals("two", action0.getList().get(1));
    assertEquals("three", action0.getList().get(2));
    assertArtifact(bin.artifact("g1:a1:1.0"), "g1:a1:1.0");

    // root
    ArtifactSet root = m.artifactSet("root");
    ProvisioArtifact b = root.artifact("org.apache.maven:apache-maven:tar.gz:bin:3.1-SNAPSHOT");
    Unpack unpack = (Unpack) b.action("unpack");
    assertNotNull(unpack);
    assertFalse(unpack.isUseRoot());
        
    UpdateJar updateJar = (UpdateJar) root.action("updateJar");
    assertNotNull(updateJar);
    assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/build.properties", updateJar.getUpdates().get("org/apache/maven/messages/build.properties"));
    assertEquals("/Users/jvanzyl/js/tesla/tesla-distribution/src/main/proviso/components.xml", updateJar.getUpdates().get("META-INF/plexus/components.xml"));
    
    assertNotNull(root.action("makeExecutable"));
    MakeExecutable me = (MakeExecutable) root.action("makeExecutable");
    assertNotNull(me);
    assertEquals("**/mvn*", me.getIncludes().get(0));
    assertEquals("**/*.bat", me.getExcludes().get(0));

    // lib
    ArtifactSet lib = m.artifactSet("lib");
    assertNotNull(lib);
    assertNotNull(lib.artifact("ch.qos.logback:logback-core:1.0.7"));
    assertNotNull(lib.artifact("ch.qos.logback:logback-classic:1.0.7"));
    assertNotNull(lib.artifact("org.eclipse.aether:aether-connector-file:0.9.0.M1"));
    assertNotNull(lib.artifact("io.tesla.aether:aether-connector-okhttp:0.0.1-SNAPSHOT"));    
    
    Validate validate = (Validate) m.action("validate");
    assertNotNull(validate);
    assertTrue(validate.isValidate());
  }
  private void assertArtifact(ProvisioArtifact artifact, String coordinate) {
    //assertEquals(g, artifact.getGroupId());
    assertEquals(coordinate, artifact.getCoordinate());
  }

  private ProvisioModel model(String model) throws Exception {
    
    return model(model, new HashMap<String,String>());
  }
  
  private ProvisioModel model(String model, Map<String,String> versionMap) throws Exception {
    String basedir = System.getProperty("basedir");
    File f = new File(basedir, "src/test/resources/" + model + ".provisio");    
    return parser.read(f, null, versionMap);
  }
  
}
