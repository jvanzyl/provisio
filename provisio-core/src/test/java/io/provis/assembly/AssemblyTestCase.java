package io.provis.assembly;

import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioModel;
import io.provis.parser.ProvisioModelParser;
import io.provis.provision.Provisioner;
import io.provis.provision.ProvisioningRequest;
import io.provis.provision.ProvisioningResult;
import io.provis.provision.VersionMapFromPom;
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

public abstract class AssemblyTestCase extends InjectedTestCase {

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
  
  public void testNothing() {    
  }
  
  public void XtestRuntimeAssembly() throws Exception {
        
    outputDirectory = getOutputDirectory();
    File assemblyDirectory = outputDirectory;
    if (assemblyDirectory.exists()) {
      FileUtils.deleteDirectory(assemblyDirectory);
    }

    Map<String,String> versionMap = versionMap();
    ProvisioModel assembly = model(name(), versionMap);
    
    validateAssembly(assembly);
    
    ProvisioningRequest request = new ProvisioningRequest()
      .setOutputDirectory(assemblyDirectory)
      .setRuntimeAssembly(assembly);

    ProvisioningResult result = provisioner.provision(request);
    
    validateRuntime();
  }
  
  protected abstract String name();
  
  protected Map<String,String> versionMap() throws Exception {
    return new HashMap<String,String>();
  }

  private ProvisioModel model(String model, Map<String,String> versionMap) throws Exception {
    File f = new File("/Users/jvanzyl/js/tesla/tesla-distribution/tesla-runtime/src/main/provisio/tesla.provisio");
    //File f = new File(provisioDescriptors, model + ".provisio");
    return parser.read(Files.newReaderSupplier(f, Charsets.UTF_8), outputDirectory, versionMap);
  }  
      
  protected void assertArtifact(ProvisioArtifact artifact, String coordinate) {
    //assertEquals(g, artifact.getGroupId());
    assertEquals(coordinate, artifact.getCoordinate());
  }
  
  //
  //
  //
  protected abstract void validateAssembly(ProvisioModel model);
  
  protected abstract void validateRuntime();  
  
}
