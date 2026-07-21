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

import static ca.vanzyl.provisio.ProvisioUtils.targetArtifactFileName;

import ca.vanzyl.provisio.action.artifact.UnpackAction;
import ca.vanzyl.provisio.action.runtime.ArchiveAction;
import ca.vanzyl.provisio.archive.SourceSpec;
import ca.vanzyl.provisio.archive.Sources;
import ca.vanzyl.provisio.model.ArtifactSet;
import ca.vanzyl.provisio.model.Directory;
import ca.vanzyl.provisio.model.FileSet;
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.Resource;
import ca.vanzyl.provisio.model.ResourceSet;
import ca.vanzyl.provisio.model.Runtime;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ArchiveAssemblyPlan {

    private final List<SourceSpec> sources = new ArrayList<>();

    static boolean isStructurallyEligible(ProvisioningContext context, ArchiveAction archive) {
        Runtime runtime = context.getRequest().getRuntime();
        return runtime.getActions().size() == 1
                && runtime.getActions().get(0) == archive
                && !ProvisioVariables.allowTargetOverwrite(context);
    }

    static ArchiveAssemblyPlan create(ProvisioningContext context, ArchiveAction archive) throws IOException {
        ArchiveAssemblyPlan plan = new ArchiveAssemblyPlan();
        String root = archive.isUseRoot()
                ? normalize(context.getRequest().getOutputDirectory().getName(), false)
                : "";
        for (ArtifactSet artifactSet : context.getRequest().getRuntimeModel().getArtifactSets()) {
            if (!plan.addArtifactSet(context, artifactSet, root)) {
                return null;
            }
        }
        if (!plan.addResourceSets(context, root) || !plan.addFileSets(context, root)) {
            return null;
        }
        return plan;
    }

    List<SourceSpec> sources() {
        return List.copyOf(sources);
    }

    private boolean addArtifactSet(ProvisioningContext context, ArtifactSet artifactSet, String parentDestination) {
        String destination = join(parentDestination, normalize(artifactSet.getDirectory(), true));
        List<ProvisioArtifact> artifacts = new ArrayList<>(artifactSet.getResolvedArtifacts());
        artifacts.sort(Comparator.comparing(artifact -> artifactOrder(context, artifact)));
        for (ProvisioArtifact artifact : artifacts) {
            if (!addArtifact(context, artifact, destination)) {
                return false;
            }
        }
        for (ArtifactSet child : artifactSet.getArtifactSets()) {
            if (!addArtifactSet(context, child, destination)) {
                return false;
            }
        }
        return true;
    }

    private boolean addArtifact(ProvisioningContext context, ProvisioArtifact artifact, String destination) {
        Path file = artifact.getPath();
        if (file == null) {
            return false;
        }
        List<ProvisioningAction> actions = artifact.getActions();
        if (actions == null) {
            String target =
                    targetArtifactFileName(context, artifact, file.getFileName().toString());
            sources.add(SourceSpec.of(Sources.file(join(destination, normalize(target, false)), file)));
            return true;
        }
        if (actions.size() == 1 && actions.get(0) instanceof UnpackAction) {
            UnpackAction unpack = (UnpackAction) actions.get(0);
            if (unpack.supportsStreaming()) {
                sources.add(unpack.streamingSource(file, destination));
                return true;
            }
        }
        return false;
    }

    private boolean addResourceSets(ProvisioningContext context, String root) {
        for (ResourceSet resourceSet : context.getRequest().getRuntime().getResourceSets()) {
            if (resourceSet.getResources() == null) {
                continue;
            }
            for (Resource resource : resourceSet.getResources()) {
                Path file = Path.of(resource.getName());
                sources.add(SourceSpec.of(
                        Sources.file(join(root, normalize(file.getFileName().toString(), false)), file)));
            }
        }
        return true;
    }

    private boolean addFileSets(ProvisioningContext context, String root) throws IOException {
        for (FileSet fileSet : context.getRequest().getRuntime().getFileSets()) {
            String destination = join(root, normalize(fileSet.getDirectory(), true));
            for (ca.vanzyl.provisio.model.File file : fileSet.getFiles()) {
                if (file.getTouch() != null) {
                    return false;
                }
                Path source = Path.of(file.getPath());
                sources.add(SourceSpec.of(Sources.file(
                        join(destination, normalize(source.getFileName().toString(), false)), source)));
            }
            for (Directory directory : fileSet.getDirectories()) {
                if (directory.isFiltering() || directory.isMustache()) {
                    return false;
                }
                SelectedDirectorySource source = SelectedDirectorySource.create(directory);
                if (source == null) {
                    return false;
                }
                SourceSpec.Builder sourceSpec = SourceSpec.builder(source).flatten(directory.isFlatten());
                if (!destination.isEmpty()) {
                    sourceSpec.destinationPrefix(destination);
                }
                sources.add(sourceSpec.build());
            }
        }
        return true;
    }

    private String artifactOrder(ProvisioningContext context, ProvisioArtifact artifact) {
        Path file = artifact.getPath();
        String fileName = file != null ? file.getFileName().toString() : "";
        String target = targetArtifactFileName(context, artifact, fileName);
        return target + '\0' + artifact.toString();
    }

    private static String join(String first, String second) {
        if (first == null || first.isEmpty()) {
            return second;
        }
        if (second == null || second.isEmpty()) {
            return first;
        }
        return first + "/" + second;
    }

    private static String normalize(String value, boolean rootAlias) {
        if (value == null || value.isEmpty() || value.equals("/") || (rootAlias && value.equals("root"))) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        for (String element : value.replace('\\', '/').split("/")) {
            if (element.isEmpty() || element.equals(".")) {
                continue;
            }
            if (element.equals("..")) {
                throw new IllegalArgumentException("Archive destination escapes its root: " + value);
            }
            if (normalized.length() > 0) {
                normalized.append('/');
            }
            normalized.append(element);
        }
        return normalized.toString();
    }
}
