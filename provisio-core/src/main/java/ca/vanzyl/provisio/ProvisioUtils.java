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

import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.File;

public class ProvisioUtils {
    private static final String ELLIPSIS = "...";

    public static String coordinateToPath(ProvisioArtifact a) {

        StringBuilder path =
                new StringBuilder().append(a.getArtifactId()).append("-").append(a.getVersion());

        if (a.getClassifier() != null && !a.getClassifier().isEmpty()) {
            path.append("-").append(a.getClassifier());
        }

        path.append(".").append(a.getExtension());

        return path.toString();
    }

    public static String targetArtifactFileName(
            ProvisioningContext context, ProvisioArtifact artifact, String artifactResolvedFilename) {
        if (artifact.getName() == null && artifactResolvedFilename == null) {
            throw new IllegalArgumentException("Artifact name and file name are both null");
        }
        if (artifact.getName() != null) {
            return artifact.getName();
        }

        ProvisioVariables.FallBackTargetFileNameMode mode = ProvisioVariables.fallbackTargetFileNameMode(context);
        switch (mode) {
            case ARTIFACT_FILE_NAME:
                return artifactResolvedFilename;
            case GA:
                int maxFileNameLength = ProvisioVariables.gaMaxFileNameLength(context);
                String gaSeparator = ProvisioVariables.gaSeparator(context);
                int remaining = maxFileNameLength - artifactResolvedFilename.length() - gaSeparator.length();
                if (remaining <= 0) {
                    return abbreviateMiddle(artifactResolvedFilename, ELLIPSIS, maxFileNameLength);
                }
                return abbreviateMiddle(artifact.getGroupId(), ELLIPSIS, remaining)
                        + gaSeparator
                        + artifactResolvedFilename;
            default:
                throw new IllegalStateException("Unknown mode for fallback target file name: " + mode);
        }
    }

    /**
     * Copied from <a href="https://github.com/apache/commons-lang/blob/29ccc7665f3bc5d84155a3092ab2209a053324e6/src/main/java/org/apache/commons/lang3/StringUtils.java#L405">StringUtils.java</a>
     */
    private static String abbreviateMiddle(String str, String middle, int length) {
        if (str != null
                && !str.trim().isEmpty()
                && middle != null
                && !middle.trim().isEmpty()
                && length < str.length()
                && length >= middle.length() + 2) {
            int targetSting = length - middle.length();
            int startOffset = targetSting / 2 + targetSting % 2;
            int endOffset = str.length() - targetSting / 2;
            return str.substring(0, startOffset) + middle + str.substring(endOffset);
        } else {
            return str;
        }
    }

    /**
     * Resolves {@code child} against {@code parent} using the same semantics as
     * {@code new File(File, String)}: a child that starts with a separator is still treated as
     * relative to the parent, unlike {@link java.nio.file.Path#resolve(String)} which would
     * discard the parent entirely.
     * <p>
     * Assembly descriptors routinely use root-anchored destinations such as
     * {@code <artifactSet to="/lib"/>}, which mean "lib inside the runtime directory".
     */
    public static File resolve(File parent, String child) {
        String relative = child;
        while (!relative.isEmpty() && (relative.charAt(0) == '/' || relative.charAt(0) == File.separatorChar)) {
            relative = relative.substring(1);
        }
        return relative.isEmpty() ? parent : parent.toPath().resolve(relative).toFile();
    }
}
