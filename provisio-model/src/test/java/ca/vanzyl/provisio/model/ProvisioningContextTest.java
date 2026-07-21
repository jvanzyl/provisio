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
package ca.vanzyl.provisio.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProvisioningContextTest {

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void tracksNormalizedAbsolutePaths() throws Exception {
        Path directory = temporary.newFolder("runtime").toPath();
        ProvisioningRequest request = new ProvisioningRequest().setOutputDirectory(directory.toFile());
        ProvisioningContext context = new ProvisioningContext(request, new ProvisioningResult(request));
        Path indirect = directory.resolve("nested/../file.txt");
        Path direct = directory.resolve("file.txt").toAbsolutePath();

        assertTrue(context.layDownFile(indirect));
        assertTrue(context.isLaidDownFile(direct));
        assertFalse(context.layDownFile(direct));
        assertTrue(context.deleteLaidDownFile(direct));
        assertFalse(context.isLaidDownFile(indirect));
    }
}
