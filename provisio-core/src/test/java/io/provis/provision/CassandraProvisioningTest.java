package io.provis.provision;

import io.provis.model.ArtifactSet;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningRequest;
import io.provis.model.Runtime;

import java.io.File;

import org.junit.Test;

public class CassandraProvisioningTest extends MavenProvisioningTest {

  String runtimeName = "presto-cassandra";

  @Test
  public void validateProvisioning() throws Exception {
    // Put the file pointer in the request
    Runtime runtime = runtime(runtimeName);
    runtime.addArtifactSetReference("runtime.classpath", referenceArtifactSet());
    ProvisioningRequest request = new ProvisioningRequest();
    System.out.println(runtime);
    request.setModel(runtime);
    request.setOutputDirectory(new File(basedir, "target/" + runtimeName));
    // should i just set the remote and local
    provisioner.provision(request);
  }
  
  @Override
  protected ProvionsingConfig provisioningConfig() throws Exception {    
    return new ProvionsingConfig(new File("/tmp/monkey"), getRemoteRepositoryUrl(runtimeName));
  }
  
  private ArtifactSet referenceArtifactSet() {
    ArtifactSet artifactSet = new ArtifactSet();
    //artifactSet.addArtifact(new ProvisioArtifact("com.datastax.cassandra:cassandra-driver-core:2.0.0-rc2"));
    artifactSet.addArtifact(new ProvisioArtifact("commons-lang:commons-lang:2.6"));
    return artifactSet;
  }
}
