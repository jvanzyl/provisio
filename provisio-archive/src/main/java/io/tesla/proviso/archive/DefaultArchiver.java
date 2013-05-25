package io.tesla.proviso.archive;

import io.provis.model.RuntimeEntry;
import io.provis.model.ProvisioContext;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.SelectorUtils;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

// useRoot
// directories
//   includes
//   excludes

// There should be a full inventory of what has gone into the archive

@Named
@Singleton
public class DefaultArchiver implements Archiver {

  public void archive(File archive, File sourceDirectory, ProvisioContext context) throws ArchiveException, IOException {
    archive(archive, sourceDirectory, context, null, null, true, false);
  }

  public void archive(File archive, File sourceDirectory, ProvisioContext context, String includes, String excludes, boolean useRoot, boolean flatten) throws ArchiveException, IOException {

    String type = "";
    String archiveBaseDirectory = sourceDirectory.getName();
    OutputStream out = new FileOutputStream(archive);
    ArchiveOutputStream aos = null;

    if (archive.getName().endsWith(".zip") || archive.getName().endsWith(".jar")) {
      aos = new ZipArchiveOutputStream(new FileOutputStream(archive));
      type = "zip";
    } else if (archive.getName().endsWith(".tgz") || archive.getName().endsWith("tar.gz")) {
      aos = new TarArchiveOutputStream(new GzipCompressorOutputStream(new FileOutputStream(archive)));
      ((TarArchiveOutputStream) aos).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
      type = "targz";
    }

    if (aos == null) {
      throw new IOException("Cannot detect how to read " + archive.getName());
    }

    //TarArchiveOutputStream aos = (TarArchiveOutputStream) new ArchiveStreamFactory().createArchiveOutputStream("tar", out);

    String[] includePatterns = null;
    String[] excludePatterns = null;

    if (includes != null) {
      includePatterns = includes.split(",");
    }

    if (excludes != null) {
      excludePatterns = excludes.split(",");
    }

    DirectoryScanner ds = new DirectoryScanner();
    ds.setIncludes(includePatterns);
    ds.setExcludes(excludePatterns);
    ds.setBasedir(sourceDirectory);
    ds.setCaseSensitive(true);
    ds.scan();

    if (type.equals("targz")) {
      for (String filePath : ds.getIncludedFiles()) {
        File file = new File(sourceDirectory, filePath);
        String entryName = archiveBaseDirectory + "/" + filePath;
        if (useRoot == false) {
          entryName = entryName.substring(entryName.indexOf('/') + 1);
        }
        TarArchiveEntry entry = new TarArchiveEntry(entryName);
        entry.setSize(file.length());
        if(context.getFileEntries().get(entryName) != null) {
          entry.setMode(context.getFileEntries().get(entryName).getMode());
        }        
        aos.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file), aos);
        aos.closeArchiveEntry();
      }
    } else if (type.equals("zip")) {
      for (String filePath : ds.getIncludedFiles()) {
        File file = new File(sourceDirectory, filePath);
        String entryName = archiveBaseDirectory + "/" + filePath;
        if (useRoot == false) {
          entryName = entryName.substring(entryName.indexOf('/') + 1);
        }
        ZipArchiveEntry entry = new ZipArchiveEntry(entryName);
        entry.setSize(file.length());
        if(context.getFileEntries().get(entryName) != null) {
          entry.setUnixMode(context.getFileEntries().get(entryName).getMode());
        }        
        aos.putArchiveEntry(entry);
        IOUtils.copy(new FileInputStream(file), aos);
        aos.closeArchiveEntry();
      }
    }

    aos.finish();
    aos.close();
    out.close();
  }

  //
  //
  //

  public Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory) throws IOException {
    return unarchive(archive, outputDirectory, null, null, true, false);
  }

  public Map<String,RuntimeEntry> unarchive(File archive, File outputDirectory, String includes, String excludes, boolean useRoot, boolean flatten) throws IOException {
    //
    // These are the contributions that unpacking this archive is providing
    //
    Map<String,RuntimeEntry> entries = new HashMap<String,RuntimeEntry>();

    if (outputDirectory.exists() == false) {
      outputDirectory.mkdirs();
    }

    String[] includePatterns = null;
    String[] excludePatterns = null;

    if (includes != null) {
      includePatterns = includes.split(",");
    }

    if (excludes != null) {
      excludePatterns = excludes.split(",");
    }

    ArchiveInputStream ais = null;

    if (archive.getName().endsWith(".zip") || archive.getName().endsWith(".jar")) {
      ais = new ZipArchiveInputStream(new FileInputStream(archive));
    } else if (archive.getName().endsWith(".tgz") || archive.getName().endsWith("tar.gz")) {
      ais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archive)));
    }

    if (ais == null) {
      throw new RuntimeException("Cannot detect how to read " + archive.getName());
    }

    ArchiveEntry archiveEntry;
    while ((archiveEntry = ais.getNextEntry()) != null) {
      String entryName = archiveEntry.getName();
      if (useRoot == false) {
        entryName = entryName.substring(entryName.indexOf('/') + 1);
      }
      //
      // If we get an exclusion that matches then just carry on.
      //
      boolean excludeFromExtraction = false;
      if (excludePatterns != null) {
        for (String excludePattern : excludePatterns) {
          if (isExcluded(excludePattern, entryName)) {
            excludeFromExtraction = true;
            break;
          }
        }
      }

      if (excludeFromExtraction) {
        continue;
      }

      //
      // need useRoot = false and step in so just truncate the beginning of the name entry
      //
      boolean includeExtraction = false;
      if (includePatterns != null) {
        for (String includePattern : includePatterns) {
          if (isIncluded(includePattern, entryName)) {
            includeExtraction = true;
            break;
          }
        }
      } else {
        includeExtraction = true;
      }

      if (includeExtraction == false) {
        continue;
      }

      //
      // So with an entry we may want to take a set of entry in a set of directories and flatten them
      // into one directory, or we may want to preserve the directory structure.
      //

      if (flatten) {
        entryName = entryName.substring(entryName.lastIndexOf("/") + 1);
      } else {
        if (archiveEntry.isDirectory()) {
          createDir(new File(outputDirectory, entryName));
          continue;
        }
      }

      File outputFile = new File(outputDirectory, entryName);

      //
      // If we take an archive and flatten it into the output directory the first entry will
      // match the output directory which exists so it will cause an error trying to make it
      //
      if (outputFile.getAbsolutePath().equals(outputDirectory.getAbsolutePath())) {
        continue;
      }

      if (!outputFile.getParentFile().exists()) {
        createDir(outputFile.getParentFile());
      }

      OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
      ByteStreams.copy(ais, outputStream);
      Closeables.closeQuietly(outputStream);
      
      int mode = 0;
      if(archiveEntry instanceof ZipArchiveEntry) {
        mode = ((ZipArchiveEntry)archiveEntry).getUnixMode();
      } else if(archiveEntry instanceof TarArchiveEntry) {
        mode = ((TarArchiveEntry)archiveEntry).getMode();        
      }
      
      // Preserve the executable bit on the way out
      if(FileMode.EXECUTABLE_FILE.equals(mode)) {
        outputFile.setExecutable(true);
        System.out.println(entryName);
      }
      
      entries.put(entryName, new RuntimeEntry(entryName,mode));      
    }

    //
    // The ArchiveInputStream needs to be closed when the whole archive is processed.
    //
    Closeables.closeQuietly(ais);

    return entries;
  }

  private void createDir(File dir) {
    if (dir.exists() == false) {
      dir.mkdirs();
    }
  }

  private boolean isExcluded(String excludePattern, String entry) {
    return SelectorUtils.match(excludePattern, entry);
  }

  private boolean isIncluded(String includePattern, String entry) {
    return SelectorUtils.match(includePattern, entry);
  }
}
