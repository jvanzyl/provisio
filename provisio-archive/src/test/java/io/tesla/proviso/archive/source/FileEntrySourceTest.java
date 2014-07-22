package io.tesla.proviso.archive.source;

import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.ArchiverTest;
import io.tesla.proviso.archive.ArchiverValidator;
import io.tesla.proviso.archive.Source;
import io.tesla.proviso.archive.tar.TarGzArchiveValidator;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class FileEntrySourceTest extends ArchiverTest {

  @Test
  public void readIndividualFilesToMakeArchive() throws IOException {
    Archiver archiver = Archiver.builder() //
        .build();

    File archive = getTargetArchive("archive-from-files.tar.gz");
    Source s0 = new FileSource(getSourceFile("0.txt"));
    Source s1 = new FileSource(getSourceFile("1.txt"));
    Source s2 = new FileSource(getSourceFile("2.txt"));
    Source s3 = new FileSource(getSourceFile("3.txt"));
    Source s4 = new FileSource(getSourceFile("4.txt"));
    archiver.archive(archive, s0, s1, s2, s3, s4);

    ArchiverValidator validator = new TarGzArchiveValidator();
    
    validator.assertNumberOfEntriesInArchive(5, archive);

    validator.assertPresenceOfEntryInArchive(archive, "0.txt");
    validator.assertPresenceOfEntryInArchive(archive, "1.txt");
    validator.assertPresenceOfEntryInArchive(archive, "2.txt");
    validator.assertPresenceOfEntryInArchive(archive, "3.txt");
    validator.assertPresenceOfEntryInArchive(archive, "4.txt");

    validator.assertContentOfEntryInArchive(archive, "0.txt", "0");
    validator.assertContentOfEntryInArchive(archive, "1.txt", "1");
    validator.assertContentOfEntryInArchive(archive, "2.txt", "2");
    validator.assertContentOfEntryInArchive(archive, "3.txt", "3");
    validator.assertContentOfEntryInArchive(archive, "4.txt", "4");
  }
}
