package io.tesla.proviso.archive;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.launch.InjectedTestCase;
import org.junit.Before;
import org.junit.Test;

public class DefaultArchiverTest extends InjectedTestCase {

  @Inject
  private Archiver archiver;
  
  @Inject
  @Named("${basedir}/src/test/archives")
  private File archives;
  
  @Inject
  @Named("${basedir}/target/archives")
  private File outputDirectory;
    
  @Before
  public void foo() {
    outputDirectory.delete();
  }
  
  @Test
  public void testUnarchiver() throws Exception {
    FileUtils.deleteDirectory(outputDirectory);
    File mavenTgz = new File(archives, "apache-maven-3.0.4-bin.tar.gz");
    archiver.unarchive(mavenTgz, outputDirectory);
    assertTrue("We expected to find the root directory name of apache-maven-3.0.4", new File(outputDirectory, "apache-maven-3.0.4").exists());
  }
}
