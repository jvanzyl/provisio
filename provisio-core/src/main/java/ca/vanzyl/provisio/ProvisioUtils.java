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
import static org.apache.commons.lang3.StringUtils.abbreviateMiddle;

import ca.vanzyl.provisio.model.ProvisioArtifact;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProvisioUtils {
    private static final int MAXIMUM_FILENAME_LENGTH = 64;
    private static final String GROUP_ARTIFACT_SEPARATOR = "_";
    private static final String ELLIPSIS = "...";

    public static String coordinateToPath(ProvisioArtifact a) {

        StringBuffer path =
                new StringBuffer().append(a.getArtifactId()).append("-").append(a.getVersion());

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

    public static String targetArtifactFileName(ProvisioArtifact artifact, String name) {
        if (artifact.getName() == null && name == null) {
            throw new IllegalArgumentException("Artifact name and file name are both null");
        }

        String filenameSuffix = (artifact.getName() != null ? artifact.getName() : name);
        int remaining = MAXIMUM_FILENAME_LENGTH - filenameSuffix.length() - GROUP_ARTIFACT_SEPARATOR.length();
        if (remaining <= 0) {
            return abbreviateMiddle(filenameSuffix, ELLIPSIS, MAXIMUM_FILENAME_LENGTH);
        }
        return abbreviateMiddle(artifact.getGroupId(), ELLIPSIS, remaining) + GROUP_ARTIFACT_SEPARATOR + filenameSuffix;
    }
}
