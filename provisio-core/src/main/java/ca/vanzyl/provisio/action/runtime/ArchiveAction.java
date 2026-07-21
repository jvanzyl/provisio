/*
 * Copyright (C) 2015-2024 Jason van Zyl
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

import ca.vanzyl.provisio.archive.Archiver;
import ca.vanzyl.provisio.archive.Archiver.ArchiverBuilder;
import ca.vanzyl.provisio.archive.ContentIdentityMode;
import ca.vanzyl.provisio.archive.EntryOrder;
import ca.vanzyl.provisio.archive.ReproducibilityPolicy;
import ca.vanzyl.provisio.archive.SourceSpec;
import ca.vanzyl.provisio.archive.Sources;
import ca.vanzyl.provisio.archive.UnArchiver;
import ca.vanzyl.provisio.model.ProvisioArchive;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.File;
import java.util.List;
import org.codehaus.plexus.util.StringUtils;

public class ArchiveAction implements ProvisioningAction {

    private String name;
    private String executable;
    private File runtimeDirectory;
    private String hardLinkIncludes;
    private String hardLinkExcludes;
    private boolean streaming;
    private Integer gzipCompressionLevel;
    private Integer gzipCompressionThreads;
    // Historic behavior is to useRoot=true because this is the default for the Takari Archiver, but we
    // want to allow setting useRoot=false to eliminate the initial leading directory entry.
    private boolean useRoot = true;

    @Override
    public void execute(ProvisioningContext context) {
        SourceSpec runtime = SourceSpec.builder(Sources.directory(runtimeDirectory.toPath()))
                .useRoot(useRoot)
                .build();
        execute(context, List.of(runtime), true);
    }

    /** Creates this action's archive from an already ordered streaming assembly plan. */
    public void execute(ProvisioningContext context, List<SourceSpec> sources) {
        execute(context, sources, false);
    }

    private void execute(ProvisioningContext context, List<SourceSpec> sources, boolean materializedRuntime) {
        ArchiverBuilder builder = Archiver.builder();
        if (executable != null) {
            builder.executable(StringUtils.split(executable, ","));
        }
        if (gzipCompressionLevel != null) {
            builder.gzipCompressionLevel(gzipCompressionLevel);
        }
        if (gzipCompressionThreads != null) {
            builder.gzipCompressionThreads(gzipCompressionThreads);
        }
        Archiver archiver = builder.reproducibility(ReproducibilityPolicy.NORMALIZED)
                .entryOrder(EntryOrder.SOURCE)
                .contentIdentity(ContentIdentityMode.SIZE_AND_CRC32)
                .posixLongFileMode(true)
                .hardLinkIncludes(split(hardLinkIncludes))
                .hardLinkExcludes(split(hardLinkExcludes))
                .build();
        try {
            File archive = new File(runtimeDirectory, "../" + name).getCanonicalFile();
            archiver.archive(archive.toPath(), sources.toArray(new SourceSpec[0]));
            //
            // Right now this action has some special meaning it maybe shouldn't, but we need to know what archives are
            // produced
            // so that we can set/attach the artifacts in a MavenProject.
            //
            String extension;
            if (archive.getName().endsWith("tar.gz")) {
                extension = "tar.gz";
            } else {
                extension = "zip";
            }
            ProvisioArchive provisioArchive = new ProvisioArchive(archive, extension);
            context.getResult().addArchive(provisioArchive);

            //
            // In the case we have made a hardlinked tarball, unpack the tarball for convenience so that it can
            // be used in subsequent operations because the runtime directory itself does not contain hardlinked
            // contents. For example if you want to make a Docker image using hardlinked contents. It might
            // be better to have the runtime directory be hardlinked before tarring it up.
            //
            if (materializedRuntime && hardLinkIncludes != null) {
                UnArchiver unArchiver = UnArchiver.builder().useRoot(false).build();
                unArchiver.unarchive(archive.toPath(), new File(runtimeDirectory + "-hardlinks").toPath());
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

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    private String[] split(String s) {
        if (s == null) {
            return new String[0];
        }
        return StringUtils.split(s, ",");
    }

    public boolean isUseRoot() {
        return useRoot;
    }

    public void setUseRoot(boolean useRoot) {
        this.useRoot = useRoot;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public void setStreaming(boolean streaming) {
        this.streaming = streaming;
    }

    public Integer getGzipCompressionLevel() {
        return gzipCompressionLevel;
    }

    public void setGzipCompressionLevel(Integer gzipCompressionLevel) {
        this.gzipCompressionLevel = gzipCompressionLevel;
    }

    public Integer getGzipCompressionThreads() {
        return gzipCompressionThreads;
    }

    public void setGzipCompressionThreads(Integer gzipCompressionThreads) {
        this.gzipCompressionThreads = gzipCompressionThreads;
    }

    public void setHardLinkIncludes(String hardLinkIncludes) {
        this.hardLinkIncludes = hardLinkIncludes;
    }

    public void setHardLinkExcludes(String hardLinkExcludes) {
        this.hardLinkExcludes = hardLinkExcludes;
    }
}
