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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import ca.vanzyl.provisio.archive.Source;
import ca.vanzyl.provisio.archive.Sources;
import ca.vanzyl.provisio.archive.UnarchivingEntryProcessor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TransformingSourceTest {

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void exposesTransformedContentOnlyDuringTheCallbackAndDeletesTheSpool() throws Exception {
        Path input = temporary.newFile("input.txt").toPath();
        Files.writeString(input, "original", StandardCharsets.UTF_8);
        Path spool = temporary.newFolder("spool-success").toPath();
        UnarchivingEntryProcessor uppercase = new UnarchivingEntryProcessor() {
            @Override
            public void processStream(String name, InputStream source, java.io.OutputStream target) throws IOException {
                String value = new String(source.readAllBytes(), StandardCharsets.UTF_8);
                target.write(value.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            }
        };

        try (Source source = new TransformingSource(Sources.file(input), uppercase, spool)) {
            source.forEachEntry(entry -> {
                assertEquals("input.txt", entry.getName());
                try (InputStream content = entry.getContent().open()) {
                    assertEquals("ORIGINAL", new String(content.readAllBytes(), StandardCharsets.UTF_8));
                }
                assertEquals(-1, entry.getContent().crc32());
            });
        }

        assertDirectoryEmpty(spool);
    }

    @Test
    public void processorFailureIsPreservedAndDeletesTheSpool() throws Exception {
        Path input = temporary.newFile("failing.txt").toPath();
        Files.writeString(input, "content", StandardCharsets.UTF_8);
        Path spool = temporary.newFolder("spool-failure").toPath();
        IOException expected = new IOException("processor failed");
        UnarchivingEntryProcessor failing = new UnarchivingEntryProcessor() {
            @Override
            public void processStream(String name, InputStream source, java.io.OutputStream target) throws IOException {
                target.write(source.read());
                throw expected;
            }
        };

        try (Source source = new TransformingSource(Sources.file(input), failing, spool)) {
            IOException actual = assertThrows(IOException.class, () -> source.forEachEntry(entry -> {}));
            assertSame(expected, actual);
        }

        assertDirectoryEmpty(spool);
    }

    @Test
    public void consumerFailureDeletesTheSpool() throws Exception {
        Path input = temporary.newFile("consumer.txt").toPath();
        Files.writeString(input, "content", StandardCharsets.UTF_8);
        Path spool = temporary.newFolder("spool-consumer-failure").toPath();
        IOException expected = new IOException("consumer failed");

        try (Source source = new TransformingSource(Sources.file(input), new UnarchivingEntryProcessor() {}, spool)) {
            IOException actual = assertThrows(
                    IOException.class,
                    () -> source.forEachEntry(entry -> {
                        try (InputStream content = entry.getContent().open()) {
                            assertEquals("content", new String(content.readAllBytes(), StandardCharsets.UTF_8));
                        }
                        throw expected;
                    }));
            assertSame(expected, actual);
        }

        assertDirectoryEmpty(spool);
    }

    @Test
    public void doesNotTransformEntriesRejectedBySelection() throws Exception {
        Path input = temporary.newFile("excluded.txt").toPath();
        Files.writeString(input, "original", StandardCharsets.UTF_8);
        Path spool = temporary.newFolder("spool-excluded").toPath();
        UnarchivingEntryProcessor failing = new UnarchivingEntryProcessor() {
            @Override
            public void processStream(String name, InputStream source, java.io.OutputStream target) throws IOException {
                throw new IOException("excluded content must not be transformed");
            }
        };

        try (Source source = new TransformingSource(Sources.file(input), failing, name -> false, spool)) {
            source.forEachEntry(entry -> {
                try (InputStream content = entry.getContent().open()) {
                    assertEquals("original", new String(content.readAllBytes(), StandardCharsets.UTF_8));
                }
            });
        }

        assertDirectoryEmpty(spool);
    }

    private void assertDirectoryEmpty(Path directory) throws IOException {
        try (Stream<Path> children = Files.list(directory)) {
            assertFalse(children.findAny().isPresent());
        }
    }
}
