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
package ca.vanzyl.provisio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ca.vanzyl.provisio.action.artifact.UnpackAction;
import ca.vanzyl.provisio.action.runtime.ArchiveAction;
import ca.vanzyl.provisio.archive.UnArchiver;
import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import ca.vanzyl.provisio.model.Runtime;
import ca.vanzyl.provisio.model.io.RuntimeReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MavenProvisionerStreamingTest {

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void streamsEligibleArtifactsWithoutStagingAndMatchesStagedOutput() throws Exception {
        Path workspace = temporary.newFolder("assembly").toPath();
        Path first = zip(
                workspace.resolve("first.zip"),
                entries("first-root/bin/run", "run", "first-root/lib/shared.jar", "shared"));
        Path second = zip(
                workspace.resolve("second.zip"),
                entries("second-root/lib/other.jar", "shared", "second-root/plugin.txt", "plugin"));

        Path streamingOutput = workspace.resolve("streaming/distribution");
        ProvisioningResult streaming = provision(streamingOutput, first, second, true);
        Path streamingArchive = workspace.resolve("streaming/distribution.tar.gz");

        assertFalse(Files.exists(streamingOutput));
        assertEquals(
                streamingArchive.toFile().getCanonicalFile(),
                streaming.getArchives().get(0).file());
        Map<String, TarArchiveEntry> streamingEntries = entries(streamingArchive);
        assertEquals(
                0755, streamingEntries.get("distribution/plugin/first/bin/run").getMode());
        assertTrue(
                streamingEntries.get("distribution/plugin/second/lib/other.jar").isLink());
        assertEquals(
                "distribution/plugin/first/lib/shared.jar",
                streamingEntries.get("distribution/plugin/second/lib/other.jar").getLinkName());

        Path stagedOutput = workspace.resolve("staged/distribution");
        provision(stagedOutput, first, second, false);
        Path stagedArchive = workspace.resolve("staged/distribution.tar.gz");
        Path streamingTree = workspace.resolve("streaming-tree");
        Path stagedTree = workspace.resolve("staged-tree");
        extract(streamingArchive, streamingTree);
        extract(stagedArchive, stagedTree);
        assertTreesEqual(stagedTree, streamingTree);

        Path repeatOutput = workspace.resolve("repeat/distribution");
        provision(repeatOutput, first, second, true);
        assertArrayEquals(
                Files.readAllBytes(streamingArchive),
                Files.readAllBytes(workspace.resolve("repeat/distribution.tar.gz")));
    }

    @Test
    public void streamsSelectedDirectoriesLooseFilesAndResourcesFromTheDescriptor() throws Exception {
        Path workspace = temporary.newFolder("file-sets").toPath();
        Path directory = workspace.resolve("source-directory");
        write(directory.resolve("config/app.properties"), "property=true");
        write(directory.resolve("config/ignored.txt"), "ignored");
        Path loose = write(workspace.resolve("loose.txt"), "loose");
        Path notice = write(workspace.resolve("NOTICE"), "notice");
        Path output = workspace.resolve("target/files-runtime");
        String descriptor = "<runtime>"
                + "<archive name=\"files-runtime.tar.gz\" streaming=\"true\"/>"
                + "<resourceSet><resource name=\"" + notice + "\"/></resourceSet>"
                + "<fileSet to=\"etc\"><file path=\"" + loose + "\"/>"
                + "<directory path=\"" + directory + "\"><include>**/*.properties</include></directory>"
                + "</fileSet></runtime>";
        Runtime runtime = readRuntime(descriptor);
        assertTrue(((ArchiveAction) runtime.getActions().get(0)).isStreaming());
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        assertFalse(Files.exists(output));
        Path extracted = workspace.resolve("extracted");
        extract(workspace.resolve("target/files-runtime.tar.gz"), extracted);
        assertEquals("notice", Files.readString(extracted.resolve("NOTICE")));
        assertEquals("loose", Files.readString(extracted.resolve("etc/loose.txt")));
        assertEquals("property=true", Files.readString(extracted.resolve("etc/config/app.properties")));
        assertFalse(Files.exists(extracted.resolve("etc/config/ignored.txt")));
    }

    @Test
    public void transformingFileSetFallsBackToMaterializedAssembly() throws Exception {
        Path workspace = temporary.newFolder("filter-fallback").toPath();
        Path directory = workspace.resolve("templates");
        write(directory.resolve("configuration.txt"), "value=${value}");
        Path output = workspace.resolve("target/fallback-runtime");
        String descriptor = "<runtime>"
                + "<archive name=\"fallback-runtime.tar.gz\" streaming=\"true\"/>"
                + "<fileSet to=\"etc\"><directory path=\"" + directory + "\" filtering=\"true\"/>"
                + "</fileSet></runtime>";
        ProvisioningRequest request = new ProvisioningRequest()
                .setRuntimeDescriptor(readRuntime(descriptor))
                .setOutputDirectory(output.toFile());
        request.setVariables(Collections.singletonMap("value", "filtered"));

        provisioner().provision(request);

        assertTrue(Files.isDirectory(output));
        assertEquals("value=filtered", Files.readString(output.resolve("etc/configuration.txt")));
        Path extracted = workspace.resolve("extracted");
        extract(workspace.resolve("target/fallback-runtime.tar.gz"), extracted);
        assertEquals("value=filtered", Files.readString(extracted.resolve("etc/configuration.txt")));
    }

    private ProvisioningResult provision(Path output, Path first, Path second, boolean streaming) throws Exception {
        Runtime runtime = new Runtime();
        runtime.addArtifactSet(artifactSet("plugin/first", "test:first:zip:1", first));
        runtime.addArtifactSet(artifactSet("plugin/second", "test:second:zip:1", second));
        ArchiveAction archive = new ArchiveAction();
        archive.setName("distribution.tar.gz");
        archive.setStreaming(streaming);
        archive.setExecutable("distribution/plugin/first/bin/run");
        archive.setHardLinkIncludes("**/*.jar");
        runtime.addAction(archive);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        return provisioner().provision(request);
    }

    private ArtifactSet artifactSet(String destination, String coordinate, Path archive) {
        UnpackAction unpack = new UnpackAction();
        unpack.setUseRoot(false);
        ProvisioArtifact artifact = new ProvisioArtifact(coordinate);
        artifact.addAction(unpack);
        ArtifactSet artifactSet = new ArtifactSet();
        artifactSet.setDirectory(destination);
        artifactSet.addArtifact(artifact.setFile(archive.toFile()));
        return artifactSet;
    }

    private MavenProvisioner provisioner() {
        RepositorySystem repositorySystem = (RepositorySystem) Proxy.newProxyInstance(
                RepositorySystem.class.getClassLoader(),
                new Class<?>[] {RepositorySystem.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("resolveDependencies")) {
                        return new DependencyResult((DependencyRequest) arguments[1]);
                    }
                    if (method.getName().equals("toString")) {
                        return "empty repository system";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        return new MavenProvisioner(repositorySystem, null, Collections.emptyList());
    }

    private Runtime readRuntime(String descriptor) throws IOException {
        RuntimeReader reader = new RuntimeReader(Actions.defaultActionDescriptors());
        return reader.read(new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)));
    }

    private Path write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        return file;
    }

    private Path zip(Path archive, Map<String, String> values) throws IOException {
        Files.createDirectories(archive.getParent());
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(archive)) {
            for (Map.Entry<String, String> value : values.entrySet()) {
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

    private void extract(Path archive, Path destination) throws IOException {
        UnArchiver.builder().useRoot(false).build().unarchive(archive, destination);
    }

    private void assertTreesEqual(Path expected, Path actual) throws IOException {
        List<String> expectedPaths = relativePaths(expected);
        assertEquals(expectedPaths, relativePaths(actual));
        for (String relative : expectedPaths) {
            Path expectedPath = expected.resolve(relative);
            Path actualPath = actual.resolve(relative);
            if (Files.isRegularFile(expectedPath)) {
                assertArrayEquals(Files.readAllBytes(expectedPath), Files.readAllBytes(actualPath));
                assertEquals(Files.isExecutable(expectedPath), Files.isExecutable(actualPath));
            }
        }
    }

    private List<String> relativePaths(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> !path.equals(root))
                    .map(root::relativize)
                    .map(Path::toString)
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }
}
