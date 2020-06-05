package ca.vanzyl.provisio.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import ca.vanzyl.provisio.model.action.Archive;
import ca.vanzyl.provisio.model.action.Unpack;
import ca.vanzyl.provisio.model.action.alter.Alter;
import ca.vanzyl.provisio.model.action.alter.Delete;
import ca.vanzyl.provisio.model.action.alter.Insert;
import ca.vanzyl.provisio.model.io.RuntimeReader;
import com.thoughtworks.xstream.converters.ConversionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.rules.ExpectedException;

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
  public void validateRuntimeUsingFileSetsWithFlattenedDirectories() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-flatten.xml")));
    List<FileSet> fileSets = runtime.getFileSets();
    FileSet conf = fileSets.get(0);
    assertEquals("concord", conf.getDirectory());
    Directory directory = conf.getDirectories().get(0);
    assertEquals("${basedir}/k8s-import", directory.getPath());
    assertEquals("**/concord-k8s*.yml", directory.getIncludes().get(0));
    assertTrue(directory.isFlatten());
  }

  @Test
  public void validateRuntimeUsingFileSet() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/assembly-with-filesets.xml")));
    List<FileSet> fileSets = runtime.getFileSets();
    FileSet bin = fileSets.get(0);
    assertEquals("bin", bin.getDirectory());
    assertEquals("/path/to/file0", bin.getFiles().get(0).getPath());
    FileSet conf = fileSets.get(1);
    assertEquals("conf", conf.getDirectory());
    Directory directory = conf.getDirectories().get(0);
    assertEquals("${basedir}/src/team/conf", directory.getPath());
    assertEquals("**/*.xml", directory.getIncludes().get(0));
    assertEquals("**/pom.xml", directory.getExcludes().get(0));
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

  @Test
  public void validateRuntimeUsingArtifactReference() throws IOException {
    Map<String, String> variables = Maps.newHashMap();
    variables.put("mavenVersion", "3.2.3");
    variables.put("tdmVersion", "3.2.1");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/runtime-with-artifact-ref.xml")), variables);
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/.mvn/wrapper", artifactSet.getDirectory());
    List<ProvisioArtifact> artifacts = artifactSet.getArtifacts();
    assertEquals(1, artifacts.size());
    ProvisioArtifact artifact = artifacts.get(0);
    assertEquals("this", artifact.getReference());
    assertEquals("wrapper.jar", artifact.getName());
  }
        
  @Test
  public void validateFileSetToAttributeIsUsed() throws IOException {
    Runtime runtime = runtime("it-0002");
    FileSet fileSet = runtime.getFileSets().get(0);    
    assertEquals("${basedir}/src/main/etc/jvm.config", fileSet.getFiles().get(0).getPath());
    assertEquals("${basedir}/src/main/etc/config.properties", fileSet.getFiles().get(1).getPath());
    assertEquals("${basedir}/src/main/etc", fileSet.getDirectories().get(0).getPath());
    assertEquals("**/*.properties", fileSet.getDirectories().get(0).getIncludes().get(0));
  }  
  
  @Test
  public void validateRuntimeInsert() throws IOException {
    Runtime runtime = runtime("it-0003");
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/lib", artifactSet.getDirectory());
    List<ProvisioArtifact> artifacts = artifactSet.getArtifacts();
    assertEquals(1, artifacts.size());
    ProvisioArtifact artifact = artifacts.get(0);
    List<ProvisioningAction> actions = artifact.getActions();
    assertEquals(1, actions.size());
    Alter alter = (Alter) actions.get(0);
    assertEquals(1, alter.getInserts().size());
    Insert insert = alter.getInserts().get(0);
    Assert.assertEquals("junit:junit:4.12", insert.getArtifacts().get(0).getCoordinate());
    Assert.assertEquals("/WEB-INF/lib/junit-4.12.jar", insert.getArtifacts().get(0).getName());
  }

  @Test
  public void validateRuntimeDelete() throws IOException {
    Runtime runtime = runtime("it-0004");
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/lib", artifactSet.getDirectory());
    List<ProvisioArtifact> artifacts = artifactSet.getArtifacts();
    assertEquals(1, artifacts.size());
    ProvisioArtifact artifact = artifacts.get(0);
    List<ProvisioningAction> actions = artifact.getActions();
    assertEquals(1, actions.size());
    Alter alter = (Alter) actions.get(0);
    assertEquals(1, alter.getDeletes().size());
    Delete delete = alter.getDeletes().get(0);
    Assert.assertEquals("/WEB-INF/lib/hudson-core-3.3.3.jar", delete.getFiles().get(0).getPath());
  }
  
  @Test
  public void validateRuntimeUsingArtifactWithExclude() throws IOException {
    Runtime runtime = runtime("it-0006");    
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/lib", artifactSet.getDirectory());
    List<ProvisioArtifact> artifacts = artifactSet.getArtifacts();
    assertEquals(1, artifacts.size());
    ProvisioArtifact artifact = artifacts.get(0);
    assertEquals("org.apache.maven:maven-core:3.3.9", artifact.getCoordinate());    
    assertEquals("org.codehaus.plexus:plexus-utils", artifact.getExclusions().get(0));
    assertEquals("org.apache.maven:maven-model", artifact.getExclusions().get(1));
  }  
  
  @Test
  public void validateRuntimeUsingArtifactSetWithExclude() throws IOException {
    Runtime runtime = runtime("it-0007");
    List<ArtifactSet> artifactSets = runtime.getArtifactSets();
    assertEquals(1, artifactSets.size());
    ArtifactSet artifactSet = artifactSets.get(0);
    assertEquals("/lib", artifactSet.getDirectory());
    assertEquals(2, artifactSet.getExcludes().size());
    assertEquals("org.codehaus.plexus:plexus-utils", artifactSet.getExcludes().get(0).getId());
    assertEquals("org.apache.maven:maven-model", artifactSet.getExcludes().get(1).getId());   
    List<ProvisioArtifact> artifacts = artifactSet.getArtifacts();
    assertEquals(2, artifacts.size());
    assertEquals("org.codehaus.modello:modello-core:1.8.3", artifacts.get(0).getCoordinate());
    assertEquals("org.apache.maven:maven-core:3.3.9", artifacts.get(1).getCoordinate());
  }

  @Rule
  public ExpectedException invalidActionException = ExpectedException.none();

  @Test
  public void validateRuntimeWithInvalidActionElementsReportErrors() throws IOException {
    invalidActionException.expect(ConversionException.class);
    invalidActionException.expectMessage("The element 'invalid' is invalid inside the <runtime/> context.");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/runtime-with-invalid-action.xml")));
  }

  @Test
  public void validateRuntimeWithArtifactSetProvidedBom() throws IOException {
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/concord-plugin.xml")));
    ArtifactSet artifactSet = runtime.getArtifactSets().get(0);
    assertEquals("com.walmartlabs.concord:concord-targetplatform:pom:1.44.0", artifactSet.getProvidedBom());
  }

  @Rule
  public ExpectedException invalidArtifactActionException = ExpectedException.none();

  @Test
  public void validateRuntimeWithInvalidArtifactActionElementsReportErrors() throws IOException {
    invalidArtifactActionException.expect(ConversionException.class);
    invalidArtifactActionException.expectMessage("The element 'invalid' is invalid inside the <artifact/> context.");
    RuntimeReader reader = new RuntimeReader(actionDescriptors());
    Runtime runtime = reader.read(new FileInputStream(new File("src/test/runtimes/runtime-with-invalid-artifact-action.xml")));
  }

  private Runtime runtime(String name) throws IOException {
    return parseDescriptor(new File(String.format("src/test/runtimes/%s/provisio.xml", name)));      
  }
  
  private Runtime parseDescriptor(File descriptor, Map<String,String> variables) throws IOException {
    RuntimeReader parser = new RuntimeReader(actionDescriptors(), variables);
    try(InputStream is = new FileInputStream(descriptor)) {
      return parser.read(is, variables);      
    }
  }

  private Runtime parseDescriptor(File descriptor) throws IOException {
    return parseDescriptor(descriptor, Maps.<String,String>newHashMap());
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
    actionDescriptors.add(new ActionDescriptor() {

      @Override
      public String getName() {
        return "alter";
      }

      @Override
      public Class<?> getImplementation() {
        return Alter.class;
      }

      @Override
      public String[] attributes() {
        return new String[] {};
      }

      @Override
      public List<Alias> aliases() {
        return ImmutableList.of(new Alias("insert", Insert.class), new Alias("delete", Delete.class));
      }

      @Override
      public List<Implicit> implicits() {
        return ImmutableList.of(
            new Implicit("inserts", Alter.class, Insert.class), new Implicit("artifacts", Insert.class),
            new Implicit("deletes", Alter.class, Delete.class), new Implicit("files", Delete.class));
      }      
    });
    return actionDescriptors;
  }
}
