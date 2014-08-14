package io.provis.provision.action.artifact;

import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.model.io.InterpolatingInputStream;
import io.tesla.proviso.archive.UnArchiver;
import io.tesla.proviso.archive.UnarchivingEntryProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.inject.Named;

import org.eclipse.aether.artifact.Artifact;

import com.google.common.io.ByteStreams;

/**
 * The unpack is an operation that results in any number of artifacts and resources being contributed to the runtime. The archive to be unpacked can
 * make the metadata about its contents available, or we need to determine the information about the contents by examining the contents.
 * 
 * @author jvanzyl
 *
 */
@Named("unpack")
public class UnpackAction implements ProvisioningAction {
  private String includes;
  private String excludes;
  private boolean useRoot;
  private boolean flatten;
  private boolean filter;
  //
  private Artifact artifact;
  private File outputDirectory;
        
  public void execute(ProvisioningContext context) {
    
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    File archive = artifact.getFile();
        
    try {
      
      UnArchiver unarchiver = UnArchiver.builder()
        .includes(includes)
        .excludes(excludes)
        .useRoot(useRoot)
        .flatten(flatten)
        .build();

      if (filter) {
        unarchiver.unarchive(archive, outputDirectory, new FilteringProcessor(context.getVariables()));
      } else {
        unarchiver.unarchive(archive, outputDirectory);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  public boolean isUseRoot() {
    return useRoot;
  }

  public void setUseRoot(boolean useRoot) {
    this.useRoot = useRoot;
  }

  public boolean isFlatten() {
    return flatten;
  }

  public void setFlatten(boolean flatten) {
    this.flatten = flatten;
  }

  public Artifact getArtifact() {
    return artifact;
  }

  public void setArtifact(Artifact artifact) {
    this.artifact = artifact;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }
  
  public boolean isFilter() {
    return filter;
  }

  public void setFilter(boolean filter) {
    this.filter = filter;
  }

  class FilteringProcessor implements UnarchivingEntryProcessor {

    Map<String,String> variables;
    
    FilteringProcessor(Map<String,String> variables) {
      this.variables = variables;
    }
    
    @Override
    public String processName(String name) {
      return name;
    }

    @Override
    public void processStream(InputStream inputStream, OutputStream outputStream) throws IOException {
      ByteStreams.copy(new InterpolatingInputStream(inputStream, variables), outputStream);            
    }    
  }
}