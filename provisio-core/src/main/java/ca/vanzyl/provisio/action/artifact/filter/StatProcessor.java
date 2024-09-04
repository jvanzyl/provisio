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
package ca.vanzyl.provisio.action.artifact.filter;

import static java.util.Objects.requireNonNull;

import ca.vanzyl.provisio.ProvisioVariables;
import ca.vanzyl.provisio.ProvisioningException;
import ca.vanzyl.provisio.archive.UnarchivingEntryProcessor;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatProcessor implements UnarchivingEntryProcessor {
    private static final Logger logger = LoggerFactory.getLogger(StatProcessor.class);
    private final ProvisioningContext context;
    private final Path archive;
    private final Path outputDirectory;
    private final UnarchivingEntryProcessor delegate;

    public StatProcessor(
            ProvisioningContext context, Path archive, Path outputDirectory, UnarchivingEntryProcessor delegate) {
        this.context = requireNonNull(context);
        this.archive = requireNonNull(archive);
        this.outputDirectory = requireNonNull(outputDirectory);
        this.delegate = delegate;
    }

    @Override
    public String processName(String name) {
        String result = name;
        if (delegate != null) {
            result = delegate.processName(name);
        }
        Path targetPath = outputDirectory.resolve(name).toAbsolutePath();
        if (!targetPath.startsWith(outputDirectory)) {
            throw new IllegalArgumentException("Bad mapping of archive " + archive + " entry " + name
                    + "; would escape output directory: " + outputDirectory);
        }
        if (!context.layDownFile(targetPath)) {
            if (ProvisioVariables.allowTargetOverwrite(context)) {
                logger.warn("Conflict: archive {} entry {} overwrites existing file {}", archive, name, targetPath);
            } else {
                throw new ProvisioningException("Conflict: archive " + archive + " entry " + name
                        + " would overwrite existing file: " + targetPath);
            }
        }
        return result;
    }

    @Override
    public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
        if (delegate != null) {
            delegate.processStream(entryName, inputStream, outputStream);
        } else {
            inputStream.transferTo(outputStream);
        }
    }
}
