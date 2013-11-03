package io.provis.provision.action.fileset;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;
import io.provis.model.RuntimeEntry;
import io.tesla.proviso.archive.FileMode;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Named;

import org.codehaus.plexus.util.FileUtils;

@Named("executable")
public class MakeExecutableAction implements Action {

  private String includes;
  private String excludes;  
  private File fileSetDirectory;
  private File runtimeDirectory;
  
  public void execute(ProvisioContext context) throws Exception {

    if (fileSetDirectory.exists()) {
      try {
        List<String> filePaths = FileUtils.getFileNames(fileSetDirectory, includes, excludes, true);
        for (String filePath : filePaths) {
          File file = new File(filePath);
          file.setExecutable(true);
          String pathInRuntime = filePath.substring(runtimeDirectory.getParentFile().getAbsolutePath().length() + 1);
          context.getFileEntries().put(pathInRuntime, new RuntimeEntry(pathInRuntime,FileMode.EXECUTABLE_FILE.getBits()));
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public String getIncludes() {
    return includes;
  }

  public void setIncludes(String includes) {
    this.includes = includes;
  }

  public String getExcludes() {
    return excludes;
  }

  public void setExcludes(String excludes) {
    this.excludes = excludes;
  }

  public File getFileSetDirectory() {
    return fileSetDirectory;
  }

  public void setFileSetDirectory(File fileSetDirectory) {
    this.fileSetDirectory = fileSetDirectory;
  }

  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }    
}