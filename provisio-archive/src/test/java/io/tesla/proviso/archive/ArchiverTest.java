package io.tesla.proviso.archive;

import static org.junit.Assert.assertTrue;

import java.io.File;

public abstract class ArchiverTest {

  private String basedir;

  //
  // Assertions for tests
  //  
  protected void assertDirectoryExists(File outputDirectory, String directoryName) {
    File directory = new File(outputDirectory, directoryName);
    assertTrue(String.format("We expected to find the directory %s, but it doesn't exist or is not a directory.", directoryName), directory.exists() && directory.isDirectory());
  }

  protected void assertFilesExists(File outputDirectory, String fileName) {
    File file = new File(outputDirectory, fileName);
    assertTrue(String.format("We expected to find the file %s, but it doesn't exist or is not a file.", fileName), file.exists() && file.isFile());
  }

  protected void assertFilesIsExecutable(File outputDirectory, String fileName) {
    File file = new File(outputDirectory, fileName);
    assertTrue(String.format("We expected to find the file %s, but it doesn't exist or is not executable.", fileName), file.exists() && file.isFile() && file.canExecute());
  }

  //
  // Helper methods for tests
  //  
  protected final String getBasedir() {
    if (null == basedir) {
      basedir = System.getProperty("basedir", new File("").getAbsolutePath());
    }
    return basedir;
  }

  protected final File getOutputDirectory() {
    return new File(getBasedir(), "target/archives");
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

  protected final File getArchiveProject(String name) {
    return new File(getBasedir(), String.format("src/test/archives/%s", name));
  }
}
