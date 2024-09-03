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

import static java.util.Objects.requireNonNull;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static long copy(InputStream from, OutputStream to) throws IOException {
        requireNonNull(from);
        requireNonNull(to);
        byte[] buf = new byte[4096];
        long total = 0L;

        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                return total;
            }

            to.write(buf, 0, r);
            total += (long) r;
        }
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
}
