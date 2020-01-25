/**
 * Copyright (C) 2015-2020 Jason van Zyl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.vanzyl.provisio.action.artifact;

import ca.vanzyl.provisio.action.artifact.filter.MustacheFilteringProcessor;
import ca.vanzyl.provisio.action.artifact.filter.StandardFilteringProcessor;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;

import javax.inject.Named;

import io.tesla.proviso.archive.UnArchiver;

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
  private boolean dereferenceHardlinks;
  private boolean mustache;
  private ProvisioArtifact artifact;
  private File outputDirectory;

  @Override
  public void execute(ProvisioningContext context) {
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }
    File archive = artifact.getFile();
    try {
      UnArchiver unarchiver = UnArchiver.builder()
        .includes(split(includes))
        .excludes(split(excludes))
        .useRoot(useRoot)
        .flatten(flatten)
        .dereferenceHardlinks(dereferenceHardlinks)
        .build();

      if (filter) {
        unarchiver.unarchive(archive, outputDirectory, new StandardFilteringProcessor(context.getVariables()));
      } else if (mustache) {
        unarchiver.unarchive(archive, outputDirectory, new MustacheFilteringProcessor(context.getVariables()));
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

  public boolean isDereferenceHardlinks() {
    return dereferenceHardlinks;
  }

  public void setDereferenceHardlinks(boolean dereferenceHardlinks) {
    this.dereferenceHardlinks = dereferenceHardlinks;
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

  public boolean isMustache() {
    return mustache;
  }

  public void setMustache(boolean mustache) {
    this.mustache = mustache;
  }
}
