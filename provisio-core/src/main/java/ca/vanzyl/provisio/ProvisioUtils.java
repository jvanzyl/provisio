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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ProvisioUtils {

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
}
