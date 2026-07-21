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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.vanzyl.provisio.ProvisioVariables;
import ca.vanzyl.provisio.ProvisioningException;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UnpackActionTest {

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void corruptArchiveDoesNotTrackOrCreateFiles() throws Exception {
        Path archive = temporary.newFile("corrupt.zip").toPath();
        Files.write(archive, "not a zip".getBytes(StandardCharsets.UTF_8));
        Path output = temporary.newFolder("corrupt-output").toPath();
        ProvisioningContext context = context(output);

        assertThrows(RuntimeException.class, () -> action(archive, output).execute(context));

        assertEquals(0, context.laidDownFiles());
        assertDirectoryEmpty(output);
    }

    @Test
    public void archivePathEscapeDoesNotTrackOrCreateFiles() throws Exception {
        Path archive = zip("escape.zip", entries("../outside.txt", "escape"));
        Path output = temporary.newFolder("escape-output").toPath();
        ProvisioningContext context = context(output);

        assertThrows(RuntimeException.class, () -> action(archive, output).execute(context));

        assertEquals(0, context.laidDownFiles());
        assertDirectoryEmpty(output);
        assertFalse(Files.exists(output.getParent().resolve("outside.txt")));
    }

    @Test
    public void failedArchiveTracksOnlyTheFileMaterializedBeforeFailure() throws Exception {
        Map<String, String> entries = entries("a.txt", "created", "z/../../outside.txt", "escape");
        Path archive = zip("partial.zip", entries);
        Path output = temporary.newFolder("partial-output").toPath();
        ProvisioningContext context = context(output);

        assertThrows(RuntimeException.class, () -> action(archive, output).execute(context));

        assertEquals("created", Files.readString(output.resolve("a.txt")));
        assertEquals(1, context.laidDownFiles());
        assertFalse(Files.exists(output.getParent().resolve("outside.txt")));
    }

    @Test
    public void conflictIsRejectedBeforeExistingFileIsOverwritten() throws Exception {
        Path first = zip("first.zip", entries("file.txt", "first"));
        Path second = zip("second.zip", entries("file.txt", "second"));
        Path output = temporary.newFolder("conflict-output").toPath();
        ProvisioningContext context = context(output);

        action(first, output).execute(context);
        assertThrows(ProvisioningException.class, () -> action(second, output).execute(context));

        assertEquals("first", Files.readString(output.resolve("file.txt")));
        assertEquals(1, context.laidDownFiles());
    }

    @Test
    public void explicitlyAllowedConflictOverwritesAndRetainsOneTrackedPath() throws Exception {
        Path first = zip("allowed-first.zip", entries("file.txt", "first"));
        Path second = zip("allowed-second.zip", entries("file.txt", "second"));
        Path output = temporary.newFolder("allowed-conflict-output").toPath();
        ProvisioningContext context = context(output, Map.of(ProvisioVariables.ALLOW_TARGET_OVERWRITE, "true"));

        action(first, output).execute(context);
        action(second, output).execute(context);

        assertEquals("second", Files.readString(output.resolve("file.txt")));
        assertEquals(1, context.laidDownFiles());
    }

    private UnpackAction action(Path archive, Path output) {
        UnpackAction action = new UnpackAction();
        action.setArtifact(new ProvisioArtifact("test:archive:zip:1").setFile(archive.toFile()));
        action.setOutputDirectory(output.toFile());
        action.setUseRoot(true);
        return action;
    }

    private ProvisioningContext context(Path output) {
        ProvisioningRequest request = new ProvisioningRequest().setOutputDirectory(output.toFile());
        return new ProvisioningContext(request, new ProvisioningResult(request));
    }

    private ProvisioningContext context(Path output, Map<String, String> variables) {
        ProvisioningRequest request = new ProvisioningRequest().setOutputDirectory(output.toFile());
        request.setVariables(variables);
        return new ProvisioningContext(request, new ProvisioningResult(request));
    }

    private Path zip(String name, Map<String, String> entries) throws IOException {
        Path archive = temporary.newFile(name).toPath();
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(archive)) {
            for (Map.Entry<String, String> value : entries.entrySet()) {
                byte[] content = value.getValue().getBytes(StandardCharsets.UTF_8);
                ZipArchiveEntry entry = new ZipArchiveEntry(value.getKey());
                entry.setSize(content.length);
                zip.putArchiveEntry(entry);
                zip.write(content);
                zip.closeArchiveEntry();
            }
        }
        return archive;
    }

    private Map<String, String> entries(String... values) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            entries.put(values[i], values[i + 1]);
        }
        return entries;
    }

    private void assertDirectoryEmpty(Path directory) throws IOException {
        try (java.util.stream.Stream<Path> children = Files.list(directory)) {
            assertTrue(children.findAny().isEmpty());
        }
    }
}
