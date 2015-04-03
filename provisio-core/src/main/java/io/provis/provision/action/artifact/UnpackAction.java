package io.provis.provision.action.artifact;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;

import io.provis.model.ProvisioArtifact;
import io.provis.model.ProvisioningAction;
import io.provis.model.ProvisioningContext;
import io.provis.model.io.InterpolatingInputStream;
import io.tesla.proviso.archive.Selector;
import io.tesla.proviso.archive.UnArchiver;
import io.tesla.proviso.archive.UnarchivingEntryProcessor;

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
  private String filterIncludes;
  private ProvisioArtifact artifact;
  private File outputDirectory;

  private static final Splitter splitter = Splitter.on(',').trimResults().omitEmptyStrings();

  @Override
  public void execute(ProvisioningContext context) {

    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    File archive = artifact.getFile();

    try {

      UnArchiver unarchiver = UnArchiver.builder() //
          .includes(split(includes)) //
          .excludes(split(excludes)) // 
          .useRoot(useRoot) //
          .flatten(flatten) //
          .build();

      if (filter) {
        if (filterIncludes != null) {
          unarchiver.unarchive(archive, outputDirectory, new SelectiveFilteringProcessor(splitter.splitToList(includes), null, context.getVariables()));
        } else {
          unarchiver.unarchive(archive, outputDirectory, new FilteringProcessor(context.getVariables()));
        }
      } else {
        unarchiver.unarchive(archive, outputDirectory);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String[] split(String s) {
    if (s == null) {
      return new String[0];
    }
    return StringUtils.split(s, ",");
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

  public ProvisioArtifact getArtifact() {
    return artifact;
  }

  public void setArtifact(ProvisioArtifact artifact) {
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

  class SelectiveFilteringProcessor implements UnarchivingEntryProcessor {

    Selector selector;
    Map<String, String> variables;

    SelectiveFilteringProcessor(List<String> includes, List<String> excludes, Map<String, String> variables) {
      this.variables = variables;
      this.selector = new Selector(includes, excludes);
    }

    @Override
    public String processName(String name) {
      return name;
    }

    @Override
    public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
      if (selector.include(entryName)) {
        ByteStreams.copy(new InterpolatingInputStream(inputStream, variables), outputStream);
      } else {
        ByteStreams.copy(inputStream, outputStream);
      }
    }
  }

  class FilteringProcessor implements UnarchivingEntryProcessor {

    Selector selector;
    Map<String, String> variables;

    FilteringProcessor(Map<String, String> variables) {
      this.variables = variables;
    }

    @Override
    public String processName(String name) {
      return name;
    }

    @Override
    public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
      ByteStreams.copy(new InterpolatingInputStream(inputStream, variables), outputStream);
    }
  }
}