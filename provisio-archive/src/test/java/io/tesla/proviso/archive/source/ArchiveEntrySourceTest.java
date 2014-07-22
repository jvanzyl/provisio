package io.tesla.proviso.archive.source;

import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.ArchiverTest;
import io.tesla.proviso.archive.ArchiverValidator;
import io.tesla.proviso.archive.Source;
import io.tesla.proviso.archive.UnArchiver;
import io.tesla.proviso.archive.tar.TarGzArchiveValidator;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class ArchiveEntrySourceTest extends ArchiverTest {

  @Test
  public void readEntriesDirectlyFromAnArchiveToMakeAnotherArchive() throws IOException {
    Archiver archiver = Archiver.builder() //
        .build();
    
    File archive = getTargetArchive("archive-from-archive.tar.gz");
    Source source = new ArchiveSource(getSourceArchive("apache-maven-3.0.4-bin.tar.gz"));
    archiver.archive(archive, source);
    
    ArchiverValidator validator = new TarGzArchiveValidator();

    validator.assertNumberOfEntriesInArchive(44, archive);
    
    // Need to make sure file modes are preserved when creating an archive from 
    // directly reading the entries of another
    File outputDirectory = new File(getOutputDirectory(), "archive-source");
    UnArchiver unArchiver = UnArchiver.builder().build();
    unArchiver.unarchive(archive, outputDirectory);
    assertDirectoryExists(outputDirectory, "apache-maven-3.0.4");
    assertFilesIsExecutable(outputDirectory, "apache-maven-3.0.4/bin/mvn");
  }
}
