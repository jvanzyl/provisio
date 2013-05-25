package io.provis.provision.action.fileset;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;
import io.tesla.proviso.archive.Archiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.io.ByteStreams;

@Named("updateJar")
public class UpdateJarAction implements Action {

  //
  // Configuration
  //
  private String jar;
  private Map<String,String> updates;
  private File fileSetDirectory;
  
  //
  // Components
  //
  private Archiver archiver;

  @Inject
  public UpdateJarAction(Archiver archiver) {
    this.archiver = archiver;
  }
  
  public String getJar() {
    return jar;
  }

  public void setJar(String jar) {
    this.jar = jar;
  }

  public Map<String, String> getUpdates() {
    return updates;
  }

  public void setUpdates(Map<String, String> updates) {
    this.updates = updates;
  }

  public File getFileSetDirectory() {
    return fileSetDirectory;
  }

  public void setFileSetDirectory(File fileSetDirectory) {
    this.fileSetDirectory = fileSetDirectory;
  }

  public Archiver getArchiver() {
    return archiver;
  }

  public void setArchiver(Archiver archiver) {
    this.archiver = archiver;
  }

  public void execute(ProvisioContext context) {

    File file = new File(fileSetDirectory, jar);
    File temp = new File(file.getParentFile(), file.getName() + ".new");
    
    try {
      JarOutputStream os = new JarOutputStream(new FileOutputStream(temp));
      JarFile jarFile = new JarFile(file);
      JarEntry jarEntry;
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        jarEntry = (JarEntry) entries.nextElement();
        InputStream is;
        String replacementForEntry = updates.get(jarEntry.getName());
        if (replacementForEntry != null) {
          //
          // Replace the entry with our new content
          //
          is = new FileInputStream(new File(replacementForEntry));
        } else {
          //
          // Just copy the entry over into the replacement JAR
          //
          is = jarFile.getInputStream(jarEntry);
        }
        add(is, jarEntry.getName(), os);
      }
      os.close();
      
      temp.renameTo(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void add(InputStream is, String entryName, JarOutputStream target) throws IOException {
    JarEntry entry = new JarEntry(entryName);
    target.putNextEntry(entry);
    ByteStreams.copy(is, target);
    target.closeEntry();
  }
}