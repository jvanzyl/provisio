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
/**
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
package ca.vanzyl.provisio.action.runtime;

import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import io.tesla.proviso.archive.Archiver;
import io.tesla.proviso.archive.Archiver.ArchiverBuilder;

import java.io.File;

import io.tesla.proviso.archive.UnArchiver;
import org.codehaus.plexus.util.StringUtils;

public class ArchiveAction implements ProvisioningAction {

  private String name;
  private String executable;
  private File runtimeDirectory;
  private String hardLinkIncludes;
  private String hardLinkExcludes;
  // Historic behavior is to useRoot=true because this is the default for the Takari Archiver, but we
  // want to allow setting useRoot=false to eliminate the initial leading directory entry.
  private boolean useRoot = true;

  public void execute(ProvisioningContext context) {
    ArchiverBuilder builder = Archiver.builder();
    if (executable != null) {
      builder.executable(StringUtils.split(executable, ","));
    }
    Archiver archiver = builder
        .posixLongFileMode(true)
        .useRoot(useRoot)
        .hardLinkIncludes(split(hardLinkIncludes))
        .hardLinkExcludes(split(hardLinkExcludes))
        .build();
    try {
      File archive = new File(runtimeDirectory, "../" + name).getCanonicalFile();
      archiver.archive(archive, runtimeDirectory);
      //
      // Right now this action has some special meaning it maybe shouldn't, but we need to know what archives are produced
      // so that we can set/attach the artifacts in a MavenProject.
      //
      context.getResult().addArchive(archive);

      //
      // In the case we have made a hardlinked tarball, unpack the tarball for convenience so that it can
      // be used in subsequent operations because the runtime directory itself does not contain hardlinked
      // contents. For example if you want to make a Docker image using hardlinked contents. It might
      // be better to have the runtime directory be hardlinked before tarring it up.
      //
      if(hardLinkIncludes != null) {
        UnArchiver unArchiver = UnArchiver
          .builder()
          .useRoot(false)
          .build();
        unArchiver.unarchive(archive, new File(runtimeDirectory + "-hardlinks"));
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public void setRuntimeDirectory(File runtimeDirectory) {
    this.runtimeDirectory = runtimeDirectory;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  private String[] split(String s) {
    if (s == null) {
      return new String[0];
    }
    return StringUtils.split(s, ",");
  }

  public boolean isUseRoot()
  {
    return useRoot;
  }

  public void setUseRoot(boolean useRoot)
  {
    this.useRoot = useRoot;
  }
}
