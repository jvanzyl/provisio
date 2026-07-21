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
package ca.vanzyl.provisio.action.artifact;

import static java.util.Objects.requireNonNull;

import ca.vanzyl.provisio.archive.EntryContents;
import ca.vanzyl.provisio.archive.EntryType;
import ca.vanzyl.provisio.archive.Source;
import ca.vanzyl.provisio.archive.SourceEntry;
import ca.vanzyl.provisio.archive.UnarchivingEntryProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/** Transforms one regular entry at a time without materializing an archive tree. */
final class TransformingSource implements Source {

    private final Source delegate;
    private final UnarchivingEntryProcessor processor;
    private final Predicate<String> selected;
    private final Path temporaryDirectory;

    TransformingSource(Source delegate, UnarchivingEntryProcessor processor) {
        this(delegate, processor, name -> true, null);
    }

    TransformingSource(Source delegate, UnarchivingEntryProcessor processor, Path temporaryDirectory) {
        this(delegate, processor, name -> true, temporaryDirectory);
    }

    TransformingSource(
            Source delegate, UnarchivingEntryProcessor processor, Predicate<String> selected, Path temporaryDirectory) {
        this.delegate = requireNonNull(delegate);
        this.processor = requireNonNull(processor);
        this.selected = requireNonNull(selected);
        this.temporaryDirectory = temporaryDirectory;
    }

    @Override
    public void forEachEntry(EntryConsumer consumer) throws IOException {
        delegate.forEachEntry(entry -> {
            if (entry.getType() != EntryType.FILE || !selected.test(entry.getName())) {
                consumer.accept(entry);
                return;
            }
            transform(entry, consumer);
        });
    }

    private void transform(SourceEntry entry, EntryConsumer consumer) throws IOException {
        Path transformed = temporaryDirectory == null
                ? Files.createTempFile("provisio-filtered-", ".tmp")
                : Files.createTempFile(temporaryDirectory, "provisio-filtered-", ".tmp");
        Throwable failure = null;
        try {
            try (InputStream input = entry.getContent().open();
                    OutputStream output = Files.newOutputStream(transformed)) {
                processor.processStream(entry.getName(), input, output);
            }
            consumer.accept(SourceEntry.file(
                    entry.getName(), EntryContents.of(transformed), entry.getFileMode(), entry.getTime()));
        } catch (IOException | RuntimeException | Error e) {
            failure = e;
            throw e;
        } finally {
            try {
                Files.deleteIfExists(transformed);
            } catch (IOException e) {
                if (failure != null) {
                    failure.addSuppressed(e);
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public boolean isDirectory() {
        return delegate.isDirectory();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }
}
