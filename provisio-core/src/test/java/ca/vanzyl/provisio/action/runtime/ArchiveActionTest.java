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
package ca.vanzyl.provisio.action.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.vanzyl.provisio.model.ProvisioArchive;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ArchiveActionTest {

    private static final long NORMALIZED_TIMESTAMP = 315_561_600_000L;

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void archivesRuntimeWithoutRootUsingNormalizedSortedEntriesAndExecutableSelection() throws Exception {
        Path runtime = temporary.newFolder("runtime").toPath();
        write(runtime, "z-last.txt", "last");
        write(runtime, "bin/run", "run");
        write(runtime, "a-first.txt", "first");
        ProvisioningRequest request = request(runtime);
        ProvisioningResult result = new ProvisioningResult(request);
        ArchiveAction action = action(runtime, "runtime.tar.gz");
        action.setUseRoot(false);
        setExecutable(action, "**/bin/run");

        action.execute(new ProvisioningContext(request, result));

        Path archive = runtime.getParent().resolve("runtime.tar.gz");
        assertTrue(Files.isRegularFile(archive));
        assertNotNull(result.getArchives());
        assertEquals(1, result.getArchives().size());
        ProvisioArchive registered = result.getArchives().get(0);
        assertEquals(archive.toFile().getCanonicalFile(), registered.file());
        assertEquals("tar.gz", registered.extension());

        Map<String, TarArchiveEntry> entries = entries(archive);
        assertEquals(list("a-first.txt", "bin/", "bin/run", "z-last.txt"), new ArrayList<>(entries.keySet()));
        assertFalse(entries.containsKey("runtime/"));
        assertEquals(
                NORMALIZED_TIMESTAMP, entries.get("a-first.txt").getModTime().getTime());
        assertEquals(0644, entries.get("a-first.txt").getMode());
        assertEquals(0755, entries.get("bin/run").getMode());
    }

    @Test
    public void archivesRuntimeWithRootByDefault() throws Exception {
        Path runtime = temporary.newFolder("rooted-runtime").toPath();
        write(runtime, "file.txt", "content");
        ProvisioningRequest request = request(runtime);
        ProvisioningResult result = new ProvisioningResult(request);

        action(runtime, "rooted.tar.gz").execute(new ProvisioningContext(request, result));

        Map<String, TarArchiveEntry> entries = entries(runtime.getParent().resolve("rooted.tar.gz"));
        assertTrue(entries.containsKey("rooted-runtime/"));
        assertTrue(entries.containsKey("rooted-runtime/file.txt"));
    }

    @Test
    public void failedArchiveIsNotCreatedOrRegistered() throws Exception {
        Path runtime = temporary.newFolder("failed-runtime").toPath();
        write(runtime, "file.txt", "content");
        ProvisioningRequest request = request(runtime);
        ProvisioningResult result = new ProvisioningResult(request);
        ArchiveAction action = action(runtime, "runtime.unsupported");

        assertThrows(RuntimeException.class, () -> action.execute(new ProvisioningContext(request, result)));

        assertFalse(Files.exists(runtime.getParent().resolve("runtime.unsupported")));
        assertNull(result.getArchives());
    }

    @Test
    public void failedArchivePreservesExistingOutputAndRemovesPartialFile() throws Exception {
        org.junit.Assume.assumeFalse(java.io.File.pathSeparatorChar == ';');
        Path runtime = temporary.newFolder("partial-runtime").toPath();
        write(runtime, "a-first.txt", "first");
        Path unreadable = runtime.resolve("z-unreadable");
        Files.createFile(unreadable);
        Files.setPosixFilePermissions(unreadable, Collections.emptySet());
        Path archive = runtime.getParent().resolve("runtime.tar.gz");
        Files.write(archive, "existing".getBytes(StandardCharsets.UTF_8));
        ProvisioningRequest request = request(runtime);
        ProvisioningResult result = new ProvisioningResult(request);

        assertThrows(RuntimeException.class, () -> action(
                        runtime, archive.getFileName().toString())
                .execute(new ProvisioningContext(request, result)));

        assertEquals("existing", Files.readString(archive));
        assertNull(result.getArchives());
        try (Stream<Path> siblings = Files.list(archive.getParent())) {
            assertFalse(
                    siblings.anyMatch(path -> path.getFileName().toString().startsWith(".provisio-runtime.tar.gz-")));
        }
    }

    private ArchiveAction action(Path runtime, String name) {
        ArchiveAction action = new ArchiveAction();
        action.setRuntimeDirectory(runtime.toFile());
        action.setName(name);
        return action;
    }

    private ProvisioningRequest request(Path runtime) {
        return new ProvisioningRequest().setOutputDirectory(runtime.toFile());
    }

    private void setExecutable(ArchiveAction action, String executable) throws ReflectiveOperationException {
        java.lang.reflect.Field field = ArchiveAction.class.getDeclaredField("executable");
        field.setAccessible(true);
        field.set(action, executable);
    }

    private void write(Path root, String name, String content) throws IOException {
        Path file = root.resolve(name);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, TarArchiveEntry> entries(Path archive) throws IOException {
        Map<String, TarArchiveEntry> entries = new LinkedHashMap<>();
        try (InputStream file = Files.newInputStream(archive);
                GzipCompressorInputStream gzip = new GzipCompressorInputStream(file);
                TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextTarEntry()) != null) {
                entries.put(entry.getName(), entry);
            }
        }
        return entries;
    }

    private List<String> list(String... values) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            result.add(value);
        }
        return result;
    }
}
