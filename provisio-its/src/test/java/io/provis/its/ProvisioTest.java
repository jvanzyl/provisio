package io.provis.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import io.provis.Actions;
import io.provis.MavenProvisioner;
import io.provis.model.ProvisioningRequest;
import io.provis.model.ProvisioningResult;
import io.provis.model.Runtime;
import io.provis.model.io.RuntimeReader;
import io.tesla.proviso.archive.ArchiveValidator;
import io.tesla.proviso.archive.ZipArchiveValidator;
import io.tesla.proviso.archive.perms.FileMode;

public class ProvisioTest {

  protected File basedir;
  protected MavenProvisioner provisioner;
  protected ResolutionSystem resolutionSystem;

  @Before
  public void prepare() {
    basedir = new File(new File("").getAbsolutePath());
  }

  private ProvisioningResult provision(String name) throws Exception {
    return provision(name, new String[] {});
  }

  private ProvisioningResult provision(String name, String... remoteRepositories) throws Exception {
    File localRepository = new File(basedir, "target/localRepository");
    resolutionSystem = new ResolutionSystem(localRepository);
    if (remoteRepositories.length > 0) {
      for (String remoteRepository : remoteRepositories) {
        resolutionSystem.remoteRepository(remoteRepository);
      }
    } else {
      resolutionSystem.remoteRepository("https://repo1.maven.org/maven2");
    }
    provisioner = new MavenProvisioner(resolutionSystem.repositorySystem(), resolutionSystem.repositorySystemSession(), resolutionSystem.remoteRepositories());
    return provisioner.provision(provisioningRequest(name));
  }

  @Test
  public void validateToAttributeForFileSetAndDirectory() throws Exception {
    String name = "it-0002";
    ProvisioningResult result = provision(name);
    assertDirectoryExists(result, "etc");
    assertFileExists(result, "etc/jvm.config");
    assertFileExists(result, "etc/config.properties");
  }

  @Test
  public void validateAlterationOfJarWithInsert() throws Exception {
    String name = "it-0003";
    ProvisioningResult result = provision(name);
    File war = new File(result.getOutputDirectory(), "lib/hudson-war-3.3.3.jar");
    ArchiveValidator validator = new ZipArchiveValidator(war);
    validator.assertEntryExists("WEB-INF/lib/junit-4.12.jar");
  }

  @Test
  public void validateAlterationOfJarWithDelete() throws Exception {
    String name = "it-0004";
    ProvisioningResult result = provision(name);
    File war = new File(result.getOutputDirectory(), "lib/hudson-war-3.3.3.jar");
    ArchiveValidator validator = new ZipArchiveValidator(war);
    validator.assertEntryDoesntExist("WEB-INF/lib/hudson-core-3.3.3.jar");
  }

  @Test
  public void validateArtifactWithExclude() throws Exception {
    String name = "it-0006";
    deleteOutputDirectory(name);
    ProvisioningResult result = provision(name);
    assertFileExists(result, "lib/maven-core-3.3.9.jar");
    assertFileDoesntExists(result, "lib/plexus-utils-3.0.22.jar");
    assertFileDoesntExists(result, "lib/maven-model-3.3.9.jar");
  }

  @Test
  public void validateArtifactSetWithExclude() throws Exception {
    String name = "it-0007";
    deleteOutputDirectory(name);
    ProvisioningResult result = provision(name);
    assertFileExists(result, "lib/modello-core-1.8.3.jar");
    assertFileExists(result, "lib/maven-core-3.3.9.jar");
    // excluded from maven
    assertFileDoesntExists(result, "lib/plexus-utils-3.0.22.jar");
    assertFileDoesntExists(result, "lib/maven-model-3.3.9.jar");
    // excluded from modello
    assertFileDoesntExists(result, "lib/plexus-utils-3.0.13.jar");
  }

  @Test
  public void validateUnpackWithStandardFiltering() throws Exception {
    // Filtering resources with ${variable} embedded
    String name = "it-0008";
    deleteOutputDirectory(name);
    ProvisioningResult result = provision(name);
    assertFileExists(result, "bin/launcher.properties");
    Properties properties = properties(result, "bin/launcher.properties");
    assertEquals("com.facebook.presto.server.PrestoServer", properties.get("main-class"));
    assertEquals("presto-server", properties.get("process-name"));
  }

  protected ProvisioningRequest provisioningRequest(String name) throws Exception {
    File projectBasedir = runtimeProject(name);
    File descriptor = new File(projectBasedir, "provisio.xml");
    File outputDirectory = outputDirectory(name);
    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(outputDirectory);
    request.setRuntimeDescriptor(parseDescriptor(descriptor));
    // If there is a provisio.properties file for inserting values use it
    File propertiesFile = new File(projectBasedir, "provisio.properties");
    Map<String, String> provisioProperties = Maps.newHashMap();
    if (propertiesFile.exists()) {
      Properties properties = new Properties();
      try (InputStream is = new FileInputStream(propertiesFile)) {
        properties.load(is);
        provisioProperties.putAll(Maps.fromProperties(properties));
      }
    }
    provisioProperties.put("basedir", basedir.getAbsolutePath());
    request.setVariables(provisioProperties);
    return request;
  }

  public static Runtime parseDescriptor(File descriptor) throws Exception {
    RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), Maps.<String, String>newHashMap());
    try (InputStream is = new FileInputStream(descriptor)) {
      return parser.read(is, ImmutableMap.of("basedir", descriptor.getParentFile().getAbsolutePath()));
    }
  }

  //
  // Assertions for tests
  //
  protected void assertDirectoryExists(ProvisioningResult result, String directoryName) {
    File outputDirectory = result.getOutputDirectory();
    File directory = new File(outputDirectory, directoryName);
    assertTrue(String.format("We expect to find the directory %s, but it doesn't exist or is not a directory.", directoryName), directory.exists() && directory.isDirectory());
  }

  protected void assertDirectoryDoesNotExist(File outputDirectory, String directoryName) {
    File directory = new File(outputDirectory, directoryName);
    assertFalse(String.format("We expect not to find the directory %s, but it is there.", directoryName), directory.exists() && directory.isDirectory());
  }

  protected void assertFileExists(ProvisioningResult result, String fileName) {
    File outputDirectory = result.getOutputDirectory();
    File file = new File(outputDirectory, fileName);
    assertTrue(String.format("We expect to find the file %s, but it doesn't exist or is not a file.", fileName), file.exists() && file.isFile());
  }

  protected void assertFileDoesntExists(ProvisioningResult result, String fileName) {
    File outputDirectory = result.getOutputDirectory();
    File file = new File(outputDirectory, fileName);
    assertFalse(String.format("We don't expect to find the file %s, but it does exist.", fileName), file.exists() && file.isFile());
  }

  protected void assertPresenceAndSizeOf(File file, int size) {
    assertTrue(String.format("We expect to find the file %s, but it doesn't exist or is not a file.", file.getName()), file.exists() && file.isFile());
    assertEquals(String.format("We expect the file to be size = %s, but it not.", size), size, file.length());
  }

  protected void assertPresenceAndSizeOf(File outputDirectory, String fileName, int size) {
    File file = new File(outputDirectory, fileName);
    assertPresenceAndSizeOf(file, size);
  }

  protected void assertPresenceAndContentOf(File file, String expectedContent) throws IOException {
    assertTrue(String.format("We expect to find the file %s, but it doesn't exist or is not a file.", file.getName()), file.exists() && file.isFile());
    assertEquals(String.format("We expect the content of the file to be %s, but is not.", expectedContent), expectedContent, FileUtils.fileRead(file));
  }

  protected void assertPresenceAndContentOf(File outputDirectory, String fileName, String expectedContent) throws IOException {
    File file = new File(outputDirectory, fileName);
    assertPresenceAndContentOf(file, expectedContent);
  }

  protected void assertFileIsExecutable(File outputDirectory, String fileName) {
    File file = new File(outputDirectory, fileName);
    assertTrue(String.format("We expect to find the file %s, but it doesn't exist or is not executable.", fileName), file.exists() && file.isFile() && file.canExecute());
  }

  //
  // Helper methods for tests
  //
  protected Properties properties(ProvisioningResult result, String name) throws IOException {
    File propertiesFile = new File(result.getOutputDirectory(), name);
    Properties properties = new Properties();
    try (InputStream is = new FileInputStream(propertiesFile)) {
      properties.load(is);
    }
    return properties;
  }

  protected final File getBasedir() {
    return basedir;
  }

  protected File file(File outputDirectory, String fileName) {
    return new File(outputDirectory, fileName);
  }

  protected final File getOutputDirectory() {
    return new File(getBasedir(), "target/archives");
  }

  protected final File outputDirectory(String name) throws IOException {
    File outputDirectory = new File(getBasedir(), "target/runtimes/" + name);
    if (outputDirectory.exists()) {
      FileUtils.deleteDirectory(outputDirectory);
    }
    return outputDirectory;
  }

  protected final void deleteOutputDirectory(String name) throws IOException {
    File outputDirectory = outputDirectory(name);
    FileUtils.deleteDirectory(outputDirectory);
  }

  protected final File getSourceArchiveDirectory() {
    return new File(getBasedir(), "src/test/archives");
  }

  protected final File getSourceArchive(String name) {
    return new File(getSourceArchiveDirectory(), name);
  }

  protected final File getSourceFileDirectory() {
    return new File(getBasedir(), "src/test/files");
  }

  protected final File getSourceFile(String name) {
    return new File(getSourceFileDirectory(), name);
  }

  protected final File getTargetArchive(String name) {
    File archive = new File(getOutputDirectory(), name);
    if (!archive.getParentFile().exists()) {
      archive.getParentFile().mkdirs();
    }
    return archive;
  }

  protected final File runtimeProject(String name) {
    return new File(getBasedir(), String.format("src/test/runtimes/%s", name));
  }
}
