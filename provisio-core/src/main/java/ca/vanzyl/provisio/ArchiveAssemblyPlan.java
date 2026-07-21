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
import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import ca.vanzyl.provisio.model.Runtime;
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
                && runtime.getResourceSets().isEmpty()
                && runtime.getFileSets().isEmpty()
                && !ProvisioVariables.allowTargetOverwrite(context);
    }

    static ArchiveAssemblyPlan create(ProvisioningContext context, ArchiveAction archive) {
        ArchiveAssemblyPlan plan = new ArchiveAssemblyPlan();
        String root = archive.isUseRoot()
                ? normalize(context.getRequest().getOutputDirectory().getName(), false)
                : "";
        for (ArtifactSet artifactSet : context.getRequest().getRuntimeModel().getArtifactSets()) {
            if (!plan.addArtifactSet(context, artifactSet, root)) {
                return null;
            }
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
