package io.provis.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.provis.model.action.Archive;
import io.provis.model.action.Unpack;
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
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly.xml")));
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
  }

  @Test
  public void validateRuntimeReaderUsingVariables() throws IOException {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-variables.xml")), variables);
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
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-variables.xml")), variables);
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
    assertEquals("unpack", unpack.getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateRuntimeReaderUsingVersionMap() throws Exception {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");

    Map<String, String> versionMap = Maps.newHashMap();
    versionMap.put("io.airlift:launcher:tar.gz:bin", "0.92");
    versionMap.put("io.airlift:launcher:tar.gz:properties", "0.92");
    versionMap.put("com.facebook.presto:presto-main:jar", "0.74");
    versionMap.put("com.facebook.presto:presto-raptor:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-tpch:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-hive-hadoop1:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-hive-hadoop2:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-hive-cdh4:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-cassandra:zip", "0.74");
    versionMap.put("com.facebook.presto:presto-example-http:zip", "0.74");

    RuntimeReader reader = new RuntimeReader(actionDescriptors(), versionMap);
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-no-versions.xml")), variables);
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
    assertEquals("com.facebook.presto:presto-main:jar:0.74", artifacts.get(0).getCoordinate());

    ProvisioArtifact artifact = artifactSets.get(0).getArtifacts().get(1);
    ProvisioningAction unpack = artifact.getActions().get(0);
    assertEquals("unpack", unpack.getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateRuntimeReaderUsingVariablesAndRuntimeActions() throws IOException {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("airshipVersion", "0.92");
    variables.put("prestoVersion", "0.74");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-runtime-actions.xml")), variables);
    
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
    
    assertEquals(1, runtime.getActions().size());
    assertEquals("archive", runtime.getActions().get(0).getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateRuntimeUsingResourceSets() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-resourcesets.xml")));
    
    ResourceSet resourceSet = runtime.getResourceSets().get(0);
    assertEquals(1, resourceSet.getResources().size());
    assertEquals("${project.artifactId}-${project.version}.jar", resourceSet.getResources().get(0).getName());
    
    assertEquals(1, runtime.getActions().size());
    assertEquals("archive", runtime.getActions().get(0).getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateRuntimeUsingReferences() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-refs.xml")));
    
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/", artifactSet.getDirectory());    
    assertEquals("runtime.classpath", artifactSet.getReference());

    assertEquals(1, runtime.getActions().size());
    assertEquals("archive", runtime.getActions().get(0).getClass().getSimpleName().toLowerCase());
  }

  @Test
  public void validateAssemblyUsingChildArtifactSets() throws IOException {    
    Map<String, String> variables = Maps.newHashMap();
    variables.put("mavenVersion", "3.2.3");
    variables.put("tdmVersion", "3.2.1");

    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-child-artifactsets.xml")), variables);
    
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(4, artifactSets.size());
    
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/", artifactSet.getDirectory());    
    
    ArtifactSet artifactSetWithChildren = artifactSets.get(3);
        
    assertEquals(2, artifactSetWithChildren.getArtifactSets().size());
    ArtifactSet libExt = artifactSetWithChildren.getArtifactSets().get(0);
    assertNotNull(libExt.getParent());
    assertEquals("lib/ext", libExt.getDirectory());
    assertEquals("io.takari.aether:takari-concurrent-localrepo:0.0.7", libExt.getArtifacts().get(0).getCoordinate());
    assertEquals("io.takari.maven:takari-smart-builder:0.0.2", libExt.getArtifacts().get(1).getCoordinate());
    assertEquals("io.takari.maven:takari-workspace-reader:0.0.2", libExt.getArtifacts().get(2).getCoordinate());
    
    ArtifactSet libDelta = artifactSetWithChildren.getArtifactSets().get(1);
    assertNotNull(libDelta.getParent());    
    assertEquals("lib/delta", libDelta.getDirectory());
    assertEquals("io.takari.tdm:tdm-delta:3.2.1", libDelta.getArtifacts().get(0).getCoordinate());
    
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
    actionDescriptors.add(new ActionDescriptor() {
      @Override
      public String getName() {
        return "archive";
      }

      @Override
      public Class<?> getImplementation() {
        return Archive.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {
          "name"
        };
      }
    });
    return actionDescriptors;
  }
}
