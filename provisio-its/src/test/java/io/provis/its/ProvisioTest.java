package io.provis.its;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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
import io.tesla.proviso.archive.FileMode;
import io.tesla.proviso.archive.ZipArchiveValidator;

public class ProvisioTest {
  
  protected File basedir;
  protected MavenProvisioner provisioner;
  
  @Before
  public void prepare() {
    basedir = new File(new File("").getAbsolutePath());
    File localRepository = new File(basedir, "target/localRepository");
    ResolutionSystem s = new ResolutionSystem(localRepository);
    provisioner = new MavenProvisioner(s.repositorySystem(), s.repositorySystemSession(), s.remoteRepositories());
  }
  
  @Test
  public void validateToAttributeForFileSetAndDirectory() throws Exception {
    String name = "it-0002";
    ProvisioningResult result = provisioner.provision(provisioningRequest(name));
    assertDirectoryExists(result, "etc");
    assertFilesExists(result, "etc/jvm.config");
    assertFilesExists(result, "etc/config.properties");
  }  

  @Test
  public void validateAlterationOfJarWithInsert() throws Exception {
    String name = "it-0003";
    ProvisioningResult result = provisioner.provision(provisioningRequest(name));
    File war = new File(result.getOutputDirectory(), "lib/hudson-war-3.3.3.jar");
    ArchiveValidator validator = new ZipArchiveValidator(war);
    validator.assertEntryExists("WEB-INF/lib/junit-4.12.jar");
  }  

  @Test
  public void validateAlterationOfJarWithDelete() throws Exception {
    String name = "it-0004";
    ProvisioningResult result = provisioner.provision(provisioningRequest(name));
    File war = new File(result.getOutputDirectory(), "lib/hudson-war-3.3.3.jar");
    ArchiveValidator validator = new ZipArchiveValidator(war);
    validator.assertEntryDoesntExist("WEB-INF/lib/hudson-core-3.3.3.jar");
  }  

  protected ProvisioningRequest provisioningRequest(String name) throws Exception {
    File projectBasedir = runtimeProject(name);
    File descriptor = new File(projectBasedir, "provisio.xml");
    File outputDirectory = outputDirectory(name);
    ProvisioningRequest request = new ProvisioningRequest();
    request.setOutputDirectory(outputDirectory);
    request.setRuntimeDescriptor(parseDescriptor(descriptor));
    request.setVariables(ImmutableMap.of("basedir",basedir.getAbsolutePath()));
    return request;
  }  
  
  public static Runtime parseDescriptor(File descriptor) throws Exception {
    RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), Maps.<String,String>newHashMap());
    try(InputStream is = new FileInputStream(descriptor)) {
      return parser.read(is, ImmutableMap.of("basedir",descriptor.getParentFile().getAbsolutePath()));      
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

  protected void assertFilesExists(ProvisioningResult result, String fileName) {
    File outputDirectory = result.getOutputDirectory();
    File file = new File(outputDirectory, fileName);
    assertTrue(String.format("We expect to find the file %s, but it doesn't exist or is not a file.", fileName), file.exists() && file.isFile());
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

  protected void XassertFileMode(File outputDirectory, String string, String expectedUnix) {
    File f = new File(outputDirectory, string);
    String unix = FileMode.toUnix(FileMode.getFileMode(f));
    assertEquals(expectedUnix, unix);
  }  
}
