package io.provis.assembly;

import io.provis.model.ProvisioArtifact;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.provis.provision.MavenProvisioner;
import io.provis.provision.ProvisioningRequest;
import io.provis.provision.ProvisioningResult;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTestCase;

public abstract class AssemblyTestCase extends InjectedTestCase {

  @Inject
  private MavenProvisioner provisioner;

  @Inject
  @Named("${basedir}/src/test/resources")
  private File provisioDescriptors;
 
  @Inject
  @Named("${basedir}/target/assembly")  
  protected File baseOutputDirectory;   

  @Inject
  private RuntimeReader parser;    
  
  protected File outputDirectory;
    
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
    Runtime assembly = model(name(), versionMap);
    
    validateAssembly(assembly);
    
    ProvisioningRequest request = new ProvisioningRequest()
      .setOutputDirectory(assemblyDirectory)
      .setModel(assembly);

    ProvisioningResult result = provisioner.provision(request);
    
    validateRuntime();
  }
  
  protected abstract String name();
  
  protected Map<String,String> versionMap() throws Exception {
    return new HashMap<String,String>();
  }

  private Runtime model(String model, Map<String,String> versionMap) throws Exception {
    File f = new File("/Users/jvanzyl/js/tesla/tesla-distribution/tesla-runtime/src/main/provisio/tesla.provisio");
    //File f = new File(provisioDescriptors, model + ".provisio");
    return parser.read(new FileInputStream(f));
  }  
      
  protected void assertArtifact(ProvisioArtifact artifact, String coordinate) {
    //assertEquals(g, artifact.getGroupId());
    assertEquals(coordinate, artifact.getCoordinate());
  }
  
  //
  //
  //
  protected abstract void validateAssembly(Runtime model);
  
  protected abstract void validateRuntime();  
  
}
