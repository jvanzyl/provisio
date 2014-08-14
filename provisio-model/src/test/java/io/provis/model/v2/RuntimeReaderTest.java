package io.provis.model.v2;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;


public class RuntimeReaderTest {

  @Test
  public void validateRuntimeReader() throws IOException {
    RuntimeReader reader = new RuntimeReader();
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/presto.xml")));
    List<ArtifactSet> artifactSets  = runtime.getArtifactSets();
    assertEquals(9, artifactSets.size());
    assertEquals("bin", artifactSets.get(0).getDirectory());
    assertEquals("lib", artifactSets.get(1).getDirectory());
    assertEquals("plugin/raptor", artifactSets.get(2).getDirectory());
    assertEquals("plugin/example-http", artifactSets.get(8).getDirectory());
        
    List<Artifact> artifacts = artifactSets.get(0).getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("io.airlift:launcher:tar.gz:bin:${airshipVersion}", artifacts.get(0).getId());
    assertEquals("io.airlift:launcher:tar.gz:properties:${airshipVersion}", artifacts.get(1).getId());
    
    List<Action> actions = artifacts.get(0).getActions();
    assertEquals("unpack", actions.get(0).getId());
    assertEquals("filter", actions.get(1).getId());
    
    Map<String, String> parameters = actions.get(1).getParameters();
    assertEquals("1", parameters.get("one"));
    assertEquals("2", parameters.get("two"));
    assertEquals("3", parameters.get("three"));
  }
  
  @Test
  public void validateRuntimeReaderUsingVariables() throws IOException {
    Map<String,String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");
    RuntimeReader reader = new RuntimeReader();
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/presto.xml")), variables);
    List<ArtifactSet> artifactSets  = runtime.getArtifactSets();
    assertEquals(9, artifactSets.size());
    assertEquals("bin", artifactSets.get(0).getDirectory());
    assertEquals("lib", artifactSets.get(1).getDirectory());
    assertEquals("plugin/raptor", artifactSets.get(2).getDirectory());
    assertEquals("plugin/example-http", artifactSets.get(8).getDirectory());
        
    List<Artifact> artifacts = artifactSets.get(0).getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("io.airlift:launcher:tar.gz:bin:0.92", artifacts.get(0).getId());
    assertEquals("io.airlift:launcher:tar.gz:properties:0.92", artifacts.get(1).getId());
    
    List<Action> actions = artifacts.get(0).getActions();
    assertEquals("unpack", actions.get(0).getId());
    assertEquals("filter", actions.get(1).getId());
    
    Map<String, String> parameters = actions.get(1).getParameters();
    assertEquals("1", parameters.get("one"));
    assertEquals("2", parameters.get("two"));
    assertEquals("3", parameters.get("three"));
    
    artifacts = artifactSets.get(1).getArtifacts();
    assertEquals("com.facebook.presto:presto-main:0.74", artifacts.get(0).getId());

  }  
  
}
