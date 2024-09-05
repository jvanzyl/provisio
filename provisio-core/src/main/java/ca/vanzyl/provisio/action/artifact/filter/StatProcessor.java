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
import ca.vanzyl.provisio.archive.UnarchivingEnhancedEntryProcessor;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatProcessor implements UnarchivingEnhancedEntryProcessor {
    private static final Logger logger = LoggerFactory.getLogger(StatProcessor.class);
    private final ProvisioningContext context;
    private final Path archive;
    private final Path outputDirectory;
    private final UnarchivingEnhancedEntryProcessor delegate;

    public StatProcessor(
            ProvisioningContext context,
            Path archive,
            Path outputDirectory,
            UnarchivingEnhancedEntryProcessor delegate) {
        this.context = requireNonNull(context);
        this.archive = requireNonNull(archive);
        this.outputDirectory = requireNonNull(outputDirectory);
        this.delegate = delegate;
    }

    @Override
    public String targetName(String name) {
        String result = name;
        if (delegate != null) {
            result = delegate.targetName(name);
        }
        return result;
    }

    @Override
    public String sourceName(String name) {
        String result = name;
        if (delegate != null) {
            result = delegate.sourceName(name);
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

    @Override
    public void processed(String entryName, Path target) {
        if (!target.startsWith(outputDirectory)) {
            throw new IllegalArgumentException("Bad mapping of archive " + archive + " entry " + entryName
                    + "; would escape output directory: " + outputDirectory);
        }
        // do NOT track directories, they MAY repeat in case of "flatten"l track everything else: files, symlinks,
        // hardlinks
        if (!Files.isDirectory(target)) {
            if (!context.layDownFile(target)) {
                if (ProvisioVariables.allowTargetOverwrite(context)) {
                    logger.warn(
                            "Conflict: archive {} entry {} overwrites existing file {}", archive, entryName, target);
                } else {
                    throw new ProvisioningException("Conflict: archive " + archive + " entry " + entryName
                            + " would overwrite existing file: " + target);
                }
            }
        }
    }
}
