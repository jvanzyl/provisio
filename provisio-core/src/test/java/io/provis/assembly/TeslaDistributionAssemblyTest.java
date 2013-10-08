package io.provis.assembly;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioModel;
import io.provis.parser.ProvisioModelParser;
import io.provis.provision.Provisioner;
import io.provis.provision.ProvisioningRequest;
import io.provis.provision.ProvisioningResult;
import io.provis.provision.VersionMapFromPom;
import io.provis.provision.action.fileset.MakeExecutableAction;
import io.tesla.aether.guice.maven.MavenBehaviourModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTestCase;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.inject.Binder;

public class TeslaDistributionAssemblyTest extends InjectedTestCase {

  @Inject
  private Provisioner provisioner;

  @Inject
  @Named("${basedir}/src/test/resources")
  private File provisioDescriptors;

  @Inject
  @Named("${basedir}/target/assembly")
  protected File baseOutputDirectory;

  @Inject
  private ProvisioModelParser parser;

  @Inject
  protected VersionMapFromPom versionMapFromPom;

  protected File outputDirectory;

  @Override
  public void configure(Binder binder) {
    binder.install(new MavenBehaviourModule());
  }

  protected File getOutputDirectory() {
    return new File(baseOutputDirectory, name());
  }

  public void testRuntimeAssembly() throws Exception {

    outputDirectory = getOutputDirectory();
    File assemblyDirectory = outputDirectory;
    if (assemblyDirectory.exists()) {
      FileUtils.deleteDirectory(assemblyDirectory);
    }

    Map<String, String> versionMap = versionMap();
    ProvisioModel assembly = model(name(), versionMap);

    validateAssembly(assembly);

    ProvisioningRequest request = new ProvisioningRequest().setOutputDirectory(assemblyDirectory).setRuntimeAssembly(assembly);

    ProvisioningResult result = provisioner.provision(request);

    validateRuntime();
  }

  protected String name() {
    return "tesla";
  }

  protected Map<String, String> versionMap() throws Exception {
    return new HashMap<String, String>();
  }

  private ProvisioModel model(String model, Map<String, String> versionMap) throws Exception {
    File f = new File("/Users/jvanzyl/js/tesla/tesla-distribution/tesla-runtime/src/main/provisio/tesla.provisio");
    //File f = new File(provisioDescriptors, model + ".provisio");
    return parser.read(Files.newReaderSupplier(f, Charsets.UTF_8), outputDirectory, versionMap);
  }

  protected void assertArtifact(ProvisioArtifact artifact, String coordinate) {
    //assertEquals(g, artifact.getGroupId());
    assertEquals(coordinate, artifact.getCoordinate());
  }

  protected void validateAssembly(ProvisioModel model) {

    //
    // bin
    //
//    ArtifactSet bin = model.fileSet("bin");
//    MakeExecutableAction me = (MakeExecutableAction) bin.action("executable");
//    assertNotNull(me);
//    assertEquals("**/mvn*", me.getIncludes());
//    assertEquals("**/*.bat", me.getExcludes());

    //
    // lib
    //
//    ArtifactSet lib = model.fileSet("lib");
//    assertNotNull(lib);
//    assertNotNull(lib.artifact("ch.qos.logback:logback-core:1.0.7"));
//    assertNotNull(lib.artifact("ch.qos.logback:logback-classic:1.0.7"));
//    assertNotNull(lib.artifact("org.eclipse.aether:aether-connector-file:0.9.0.M2"));
//    assertNotNull(lib.artifact("io.tesla.aether:aether-connector-okhttp:0.0.5"));
  }

  protected void validateRuntime() {
    // Artifacts that should be here
    assertFilePresent("bin/mvn");
  //  assertFilePresent("lib/logback-core-1.0.7.jar");
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
