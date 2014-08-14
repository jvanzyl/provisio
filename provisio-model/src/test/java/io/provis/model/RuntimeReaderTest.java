package io.provis.model;

import static org.junit.Assert.assertEquals;
import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.io.RuntimeReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RuntimeReaderTest {

  @Test
  public void validateRuntimeReader() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/presto.xml")));
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(9, artifactSets.size());
    assertEquals("bin", artifactSets.get(0).getDirectory());
    assertEquals("lib", artifactSets.get(1).getDirectory());
    assertEquals("plugin/raptor", artifactSets.get(2).getDirectory());
    assertEquals("plugin/example-http", artifactSets.get(8).getDirectory());

    List<ProvisioArtifact> artifacts = artifactSets.get(0).getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("io.airlift:launcher:tar.gz:bin:${airshipVersion}", artifacts.get(0).getCoordinate());
    assertEquals("io.airlift:launcher:tar.gz:properties:${airshipVersion}", artifacts.get(1).getCoordinate());

    List<ProvisioningAction> actions = artifacts.get(0).getActions();
    assertEquals("unpack", actions.get(0).getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateRuntimeReaderUsingVariables() throws IOException {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/presto.xml")), variables);
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(9, artifactSets.size());
    assertEquals("bin", artifactSets.get(0).getDirectory());
    assertEquals("lib", artifactSets.get(1).getDirectory());
    assertEquals("plugin/raptor", artifactSets.get(2).getDirectory());
    assertEquals("plugin/example-http", artifactSets.get(8).getDirectory());

    List<ProvisioArtifact> artifacts = artifactSets.get(0).getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("io.airlift:launcher:tar.gz:bin:0.92", artifacts.get(0).getCoordinate());
    assertEquals("io.airlift:launcher:tar.gz:properties:0.92", artifacts.get(1).getCoordinate());

    List<ProvisioningAction> actions = artifacts.get(0).getActions();
    assertEquals("unpack", actions.get(0).getClass().getSimpleName().toLowerCase());

    artifacts = artifactSets.get(1).getArtifacts();
    assertEquals("com.facebook.presto:presto-main:0.74", artifacts.get(0).getCoordinate());
  }
  
  @Test
  public void validateRuntimeReaderUsingVariablesAndCustomActions() throws Exception {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/presto-with-actions.xml")), variables);
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(9, artifactSets.size());
    assertEquals("bin", artifactSets.get(0).getDirectory());
    assertEquals("lib", artifactSets.get(1).getDirectory());
    assertEquals("plugin/raptor", artifactSets.get(2).getDirectory());
    assertEquals("plugin/example-http", artifactSets.get(8).getDirectory());

    List<ProvisioArtifact> artifacts = artifactSets.get(0).getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("io.airlift:launcher:tar.gz:bin:0.92", artifacts.get(0).getCoordinate());
    assertEquals("io.airlift:launcher:tar.gz:properties:0.92", artifacts.get(1).getCoordinate());

    artifacts = artifactSets.get(1).getArtifacts();
    assertEquals("com.facebook.presto:presto-main:0.74", artifacts.get(0).getCoordinate());

    ProvisioArtifact artifact = artifactSets.get(0).getArtifacts().get(1);
    ProvisioningAction unpack = artifact.getActions().get(0);
    unpack.execute(null);
  }
  
  private List<ActionDescriptor> actionDescriptors() {
    List<ActionDescriptor> actionDescriptors = Lists.newArrayList();
    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "unpack";
      }
      @Override
      public Class<?> getImplementation() {
        return Unpack.class;
      }
      @Override
      public String[] attributes() {
        return new String[] {
          "filter", "includes", "excludes", "flatten"
        };
      }
    });
    return actionDescriptors;
  }
}
