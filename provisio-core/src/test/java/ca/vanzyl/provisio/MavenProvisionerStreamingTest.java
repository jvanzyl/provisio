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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import ca.vanzyl.provisio.action.artifact.UnpackAction;
import ca.vanzyl.provisio.action.runtime.ArchiveAction;
import ca.vanzyl.provisio.action.runtime.MakeDirectoryAction;
import ca.vanzyl.provisio.archive.UnArchiver;
import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.ProvisioningRequest;
import ca.vanzyl.provisio.model.ProvisioningResult;
import ca.vanzyl.provisio.model.Runtime;
import ca.vanzyl.provisio.model.io.RuntimeReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
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
        Path reorderedFirst = zip(
                workspace.resolve("reordered-first.zip"),
                entries("first-root/lib/shared.jar", "shared", "first-root/bin/run", "run"));
        Path reorderedSecond = zip(
                workspace.resolve("reordered-second.zip"),
                entries("second-root/plugin.txt", "plugin", "second-root/lib/other.jar", "shared"));
        provision(repeatOutput, reorderedFirst, reorderedSecond, true);
        assertArrayEquals(
                Files.readAllBytes(streamingArchive),
                Files.readAllBytes(workspace.resolve("repeat/distribution.tar.gz")));
    }

    @Test
    public void corruptStreamingInputPreservesExistingArchiveAndLeavesNoStagingTree() throws Exception {
        Path workspace = temporary.newFolder("corrupt-stream").toPath();
        Path corrupt = write(workspace.resolve("corrupt.zip"), "not a zip");
        Path output = workspace.resolve("target/distribution");
        Path archive = workspace.resolve("target/distribution.tar.gz");
        write(archive, "existing archive");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("plugin/corrupt", "test:corrupt:zip:1", corrupt));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        assertThrows(RuntimeException.class, () -> provisioner().provision(request));

        assertEquals("existing archive", Files.readString(archive));
        assertFalse(Files.exists(output));
        assertEquals(0, temporaryArchiveFiles(archive));
    }

    @Test
    public void duplicateStreamingTargetsFailTransactionally() throws Exception {
        Path workspace = temporary.newFolder("duplicate-stream").toPath();
        Path first = zip(workspace.resolve("first.zip"), entries("first/a.txt", "first"));
        Path second = zip(workspace.resolve("second.zip"), entries("second/a.txt", "second"));
        Path output = workspace.resolve("target/distribution");
        Path archive = workspace.resolve("target/distribution.tar.gz");
        write(archive, "existing archive");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("same", "test:first:zip:1", first));
        runtime.addArtifactSet(artifactSet("same", "test:second:zip:1", second));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        RuntimeException failure =
                assertThrows(RuntimeException.class, () -> provisioner().provision(request));

        assertTrue(failure.getCause().getMessage().contains("Duplicate archive entry distribution/same/a.txt"));
        assertEquals("existing archive", Files.readString(archive));
        assertFalse(Files.exists(output));
        assertEquals(0, temporaryArchiveFiles(archive));
    }

    @Test
    public void streamsFilteredTarArtifactWithoutMaterializingAssembly() throws Exception {
        Path workspace = temporary.newFolder("artifact-filter-stream").toPath();
        Path input = tarGz(
                workspace.resolve("launcher-properties.tar.gz"),
                entries("root/etc/launcher.properties", "node.environment=${environment}"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        ArtifactSet artifactSet = artifactSet("etc", "test:filtered:zip:1", input);
        ((UnpackAction) artifactSet.getArtifacts().get(0).getActions().get(0)).setFilter(true);
        runtime.addArtifactSet(artifactSet);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        request.setVariables(Map.of("environment", "testing"));

        provisioner().provision(request);

        assertFalse(Files.exists(output));
        Path extracted = workspace.resolve("extracted");
        extract(workspace.resolve("target/distribution.tar.gz"), extracted);
        assertEquals("node.environment=testing", Files.readString(extracted.resolve("etc/etc/launcher.properties")));
    }

    @Test
    public void streamsMustacheFilteredArtifactWithoutMaterializingAssembly() throws Exception {
        Path workspace = temporary.newFolder("artifact-mustache-stream").toPath();
        Path input = zip(
                workspace.resolve("input.zip"),
                entries("root/configuration.txt", "value={{value}};{{#enabled}}enabled{{/enabled}}"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        ArtifactSet artifactSet = artifactSet("etc", "test:mustache:zip:1", input);
        ((UnpackAction) artifactSet.getArtifacts().get(0).getActions().get(0)).setMustache(true);
        runtime.addArtifactSet(artifactSet);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        request.setVariables(entries("value", "filtered", "enabled", "true"));

        provisioner().provision(request);

        assertFalse(Files.exists(output));
        Path extracted = workspace.resolve("extracted");
        extract(workspace.resolve("target/distribution.tar.gz"), extracted);
        assertEquals("value=filtered;enabled", Files.readString(extracted.resolve("etc/configuration.txt")));
    }

    @Test
    public void overwriteModeFallsBackAndPreservesLastWriterSemantics() throws Exception {
        Path workspace = temporary.newFolder("overwrite-fallback").toPath();
        Path first = zip(workspace.resolve("first.zip"), entries("first/value.txt", "first"));
        Path second = zip(workspace.resolve("second.zip"), entries("second/value.txt", "second"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("same", "test:first:zip:1", first));
        runtime.addArtifactSet(artifactSet("same", "test:second:zip:1", second));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        request.setVariables(Map.of(ProvisioVariables.ALLOW_TARGET_OVERWRITE, "true"));

        provisioner().provision(request);

        assertTrue(Files.isDirectory(output));
        assertEquals("second", Files.readString(output.resolve("same/value.txt")));
    }

    @Test
    public void looseArtifactsWithoutCrcMetadataUseVerifiedHardLinkIdentity() throws Exception {
        Path workspace = temporary.newFolder("loose-artifacts").toPath();
        Path first = write(workspace.resolve("first.jar"), "shared");
        Path second = write(workspace.resolve("second.jar"), "shared");
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(looseArtifactSet("lib/first", "test:first:jar:1", first));
        runtime.addArtifactSet(looseArtifactSet("lib/second", "test:second:jar:1", second));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        assertTrue(entries.get("distribution/lib/second/second.jar").isLink());
        assertEquals(
                "distribution/lib/first/first.jar",
                entries.get("distribution/lib/second/second.jar").getLinkName());
    }

    @Test
    public void zipEntryLinksToEarlierLooseArtifactAcrossIdentityMetadata() throws Exception {
        Path workspace = temporary.newFolder("loose-before-zip").toPath();
        Path loose = write(workspace.resolve("shared.jar"), "shared");
        Path plugin = zip(workspace.resolve("plugin.zip"), entries("root/shared-copy.jar", "shared"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(looseArtifactSet("lib", "test:shared:jar:1", loose));
        runtime.addArtifactSet(artifactSet("plugin/example", "test:plugin:zip:1", plugin));

        provisioner()
                .provision(
                        new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile()));

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        TarArchiveEntry duplicate = entries.get("distribution/plugin/example/shared-copy.jar");
        assertTrue(duplicate.isLink());
        assertEquals("distribution/lib/shared.jar", duplicate.getLinkName());
    }

    @Test
    public void looseArtifactLinksToEarlierZipEntryAcrossIdentityMetadata() throws Exception {
        Path workspace = temporary.newFolder("zip-before-loose").toPath();
        Path plugin = zip(workspace.resolve("plugin.zip"), entries("root/shared.jar", "shared"));
        Path loose = write(workspace.resolve("shared-copy.jar"), "shared");
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("plugin/example", "test:plugin:zip:1", plugin));
        runtime.addArtifactSet(looseArtifactSet("lib", "test:shared:jar:1", loose));

        provisioner()
                .provision(
                        new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile()));

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        TarArchiveEntry duplicate = entries.get("distribution/lib/shared-copy.jar");
        assertTrue(duplicate.isLink());
        assertEquals("distribution/plugin/example/shared.jar", duplicate.getLinkName());
    }

    @Test
    public void sameSizeZipEntriesWithDifferentCrc32RemainIndependentFiles() throws Exception {
        Path workspace = temporary.newFolder("different-crc").toPath();
        Path first = zip(workspace.resolve("first.zip"), entries("root/first.jar", "first!"));
        Path second = zip(workspace.resolve("second.zip"), entries("root/second.jar", "second"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("lib/first", "test:first:zip:1", first));
        runtime.addArtifactSet(artifactSet("lib/second", "test:second:zip:1", second));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        assertFalse(entries.get("distribution/lib/first/first.jar").isLink());
        assertFalse(entries.get("distribution/lib/second/second.jar").isLink());
    }

    @Test
    public void streamsWithoutTheRuntimeRootWhenRequested() throws Exception {
        Path workspace = temporary.newFolder("without-runtime-root").toPath();
        Path input = zip(workspace.resolve("input.zip"), entries("root/bin/run", "run"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        ((ArchiveAction) runtime.getActions().get(0)).setUseRoot(false);
        runtime.addArtifactSet(artifactSet("application", "test:application:zip:1", input));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        assertTrue(entries.containsKey("application/bin/run"));
        assertFalse(entries.containsKey("distribution/"));
        assertFalse(Files.exists(output));
    }

    @Test
    public void streamsUnpackIncludesExcludesAndFlattening() throws Exception {
        Path workspace = temporary.newFolder("unpack-selection").toPath();
        Path input = zip(
                workspace.resolve("input.zip"),
                entries(
                        "root/keep/alpha.txt",
                        "alpha",
                        "root/keep/ignored.properties",
                        "ignored",
                        "root/skip/beta.txt",
                        "excluded"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        ArtifactSet artifactSet = artifactSet("selected", "test:selected:zip:1", input);
        UnpackAction unpack =
                (UnpackAction) artifactSet.getArtifacts().get(0).getActions().get(0);
        unpack.setIncludes("**/*.txt");
        unpack.setExcludes("**/skip/**");
        unpack.setFlatten(true);
        runtime.addArtifactSet(artifactSet);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        assertTrue(entries.containsKey("distribution/selected/alpha.txt"));
        assertFalse(entries.containsKey("distribution/selected/ignored.properties"));
        assertFalse(entries.containsKey("distribution/selected/beta.txt"));
        assertFalse(Files.exists(output));
    }

    @Test
    public void preservesTarSymbolicAndHardLinksWhileStreaming() throws Exception {
        Path workspace = temporary.newFolder("tar-links").toPath();
        Path input = tarGzWithLinks(workspace.resolve("input.tar.gz"));
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("application", "test:application:tar.gz:1", input));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        provisioner().provision(request);

        Map<String, TarArchiveEntry> entries = entries(workspace.resolve("target/distribution.tar.gz"));
        TarArchiveEntry symbolicLink = entries.get("distribution/application/bin/tool-link");
        assertTrue(symbolicLink.isSymbolicLink());
        assertEquals("tool", symbolicLink.getLinkName());
        TarArchiveEntry hardLink = entries.get("distribution/application/lib/alias.jar");
        assertTrue(hardLink.isLink());
        assertEquals("distribution/application/lib/original.jar", hardLink.getLinkName());
        assertFalse(Files.exists(output));
    }

    @Test
    public void invalidStreamingDestinationPreservesExistingArchive() throws Exception {
        Path workspace = temporary.newFolder("invalid-destination").toPath();
        Path input = zip(workspace.resolve("input.zip"), entries("root/file.txt", "content"));
        Path output = workspace.resolve("target/distribution");
        Path archive = workspace.resolve("target/distribution.tar.gz");
        write(archive, "existing archive");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("../outside", "test:escape:zip:1", input));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        IllegalArgumentException failure =
                assertThrows(IllegalArgumentException.class, () -> provisioner().provision(request));

        assertEquals("Archive destination escapes its root: ../outside", failure.getMessage());
        assertEquals("existing archive", Files.readString(archive));
        assertFalse(Files.exists(output));
        assertFalse(Files.exists(workspace.resolve("target/outside")));
        assertEquals(0, temporaryArchiveFiles(archive));
    }

    @Test
    public void escapingStreamingEntryFailsTransactionally() throws Exception {
        Path workspace = temporary.newFolder("escaping-entry").toPath();
        Path input = zip(workspace.resolve("input.zip"), entries("../outside.txt", "escape"));
        Path output = workspace.resolve("target/distribution");
        Path archive = workspace.resolve("target/distribution.tar.gz");
        write(archive, "existing archive");
        Runtime runtime = runtimeWithArchive(true);
        runtime.addArtifactSet(artifactSet("application", "test:escape:zip:1", input));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());

        assertThrows(RuntimeException.class, () -> provisioner().provision(request));

        assertEquals("existing archive", Files.readString(archive));
        assertFalse(Files.exists(output));
        assertFalse(Files.exists(workspace.resolve("target/outside.txt")));
        assertEquals(0, temporaryArchiveFiles(archive));
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
                + "<archive name=\"files-runtime.tar.gz\" streaming=\"true\" gzipCompressionLevel=\"6\""
                + " gzipCompressionThreads=\"3\"/>"
                + "<resourceSet><resource name=\"" + notice + "\"/></resourceSet>"
                + "<fileSet to=\"etc\"><file path=\"" + loose + "\"/>"
                + "<directory path=\"" + directory + "\"><include>**/*.properties</include></directory>"
                + "</fileSet></runtime>";
        Runtime runtime = readRuntime(descriptor);
        ArchiveAction archiveAction = (ArchiveAction) runtime.getActions().get(0);
        assertTrue(archiveAction.isStreaming());
        assertEquals(Integer.valueOf(6), archiveAction.getGzipCompressionLevel());
        assertEquals(Integer.valueOf(3), archiveAction.getGzipCompressionThreads());
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
        request.setVariables(Map.of("value", "filtered"));

        provisioner().provision(request);

        assertTrue(Files.isDirectory(output));
        assertEquals("value=filtered", Files.readString(output.resolve("etc/configuration.txt")));
        Path extracted = workspace.resolve("extracted");
        extract(workspace.resolve("target/fallback-runtime.tar.gz"), extracted);
        assertEquals("value=filtered", Files.readString(extracted.resolve("etc/configuration.txt")));
    }

    @Test
    public void touchFileFallsBackToMaterializedAssembly() throws Exception {
        Path workspace = temporary.newFolder("touch-fallback").toPath();
        Path output = workspace.resolve("target/distribution");
        String descriptor = "<runtime>"
                + "<archive name=\"distribution.tar.gz\" streaming=\"true\"/>"
                + "<fileSet to=\"state\"><file touch=\"ready\"/></fileSet>"
                + "</runtime>";
        Runtime runtime = readRuntime(descriptor);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        ProvisioningContext context = new ProvisioningContext(request, new ProvisioningResult(request));

        ArchiveAssemblyPlan plan = ArchiveAssemblyPlan.create(
                context, (ArchiveAction) runtime.getActions().get(0));
        assertEquals("touch file ready requires staged materialization", plan.fallbackReason());

        provisioner().provision(request);

        assertTrue(Files.isRegularFile(output.resolve("state/ready")));
        assertTrue(entries(workspace.resolve("target/distribution.tar.gz")).containsKey("distribution/state/ready"));
    }

    @Test
    public void additionalRuntimeActionFallsBackToMaterializedAssembly() throws Exception {
        Path workspace = temporary.newFolder("runtime-action-fallback").toPath();
        Path output = workspace.resolve("target/distribution");
        Runtime runtime = new Runtime();
        MakeDirectoryAction makeDirectory = new MakeDirectoryAction();
        makeDirectory.setName("generated");
        runtime.addAction(makeDirectory);
        ArchiveAction archive = new ArchiveAction();
        archive.setName("distribution.tar.gz");
        archive.setStreaming(true);
        runtime.addAction(archive);
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        ProvisioningContext context = new ProvisioningContext(request, new ProvisioningResult(request));

        assertEquals(
                "the streaming archive must be the only runtime action",
                ArchiveAssemblyPlan.structuralFallbackReason(context, archive));

        provisioner().provision(request);

        assertTrue(Files.isDirectory(output.resolve("generated")));
        assertTrue(entries(workspace.resolve("target/distribution.tar.gz")).containsKey("distribution/generated/"));
    }

    private ProvisioningResult provision(Path output, Path first, Path second, boolean streaming) throws Exception {
        Runtime runtime = runtimeWithArchive(streaming);
        runtime.addArtifactSet(artifactSet("plugin/first", "test:first:zip:1", first));
        runtime.addArtifactSet(artifactSet("plugin/second", "test:second:zip:1", second));
        ProvisioningRequest request =
                new ProvisioningRequest().setRuntimeDescriptor(runtime).setOutputDirectory(output.toFile());
        return provisioner().provision(request);
    }

    private Runtime runtimeWithArchive(boolean streaming) {
        Runtime runtime = new Runtime();
        ArchiveAction archive = new ArchiveAction();
        archive.setName("distribution.tar.gz");
        archive.setStreaming(streaming);
        archive.setExecutable("distribution/plugin/first/bin/run");
        archive.setHardLinkIncludes("**/*.jar");
        runtime.addAction(archive);
        return runtime;
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

    private ArtifactSet looseArtifactSet(String destination, String coordinate, Path file) {
        ArtifactSet artifactSet = new ArtifactSet();
        artifactSet.setDirectory(destination);
        artifactSet.addArtifact(new ProvisioArtifact(coordinate).setFile(file.toFile()));
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
        return new MavenProvisioner(repositorySystem, null, List.of());
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

    private Path tarGzWithLinks(Path archive) throws IOException {
        Files.createDirectories(archive.getParent());
        try (OutputStream file = Files.newOutputStream(archive);
                GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(file);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            tarFile(tar, "root/bin/tool", "tool");
            TarArchiveEntry symbolicLink = new TarArchiveEntry("root/bin/tool-link", TarConstants.LF_SYMLINK);
            symbolicLink.setLinkName("tool");
            tar.putArchiveEntry(symbolicLink);
            tar.closeArchiveEntry();
            tarFile(tar, "root/lib/original.jar", "content");
            TarArchiveEntry hardLink = new TarArchiveEntry("root/lib/alias.jar", TarConstants.LF_LINK);
            hardLink.setLinkName("root/lib/original.jar");
            tar.putArchiveEntry(hardLink);
            tar.closeArchiveEntry();
        }
        return archive;
    }

    private Path tarGz(Path archive, Map<String, String> values) throws IOException {
        Files.createDirectories(archive.getParent());
        try (OutputStream file = Files.newOutputStream(archive);
                GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(file);
                TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            for (Map.Entry<String, String> value : values.entrySet()) {
                tarFile(tar, value.getKey(), value.getValue());
            }
        }
        return archive;
    }

    private void tarFile(TarArchiveOutputStream tar, String name, String value) throws IOException {
        byte[] content = value.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(content.length);
        tar.putArchiveEntry(entry);
        tar.write(content);
        tar.closeArchiveEntry();
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

    private long temporaryArchiveFiles(Path archive) throws IOException {
        String prefix = ".provisio-" + archive.getFileName() + "-";
        try (Stream<Path> paths = Files.list(archive.getParent())) {
            return paths.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .count();
        }
    }
}
