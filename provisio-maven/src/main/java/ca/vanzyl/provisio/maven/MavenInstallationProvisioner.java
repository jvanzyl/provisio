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
package ca.vanzyl.provisio.maven;

import ca.vanzyl.provisio.SimpleProvisioner;
import java.io.File;
import java.io.IOException;
import javax.inject.Named;

@Named
public class MavenInstallationProvisioner extends SimpleProvisioner {

    public File provision(String mavenVersion, File installDirectory) throws IOException {
        if (mavenVersion == null || mavenVersion.length() <= 0) {
            throw new IllegalArgumentException("Maven version not specified");
        }

        File mvn = new File(installDirectory, "bin/mvn");
        // If we're working with snapshot versions re-provision
        if (mvn.exists() && !mavenVersion.contains("SNAPSHOT")) {
            return installDirectory;
        }

        File archive;
        if (mavenVersion.contains(":")) {
            // We have a coordinate
            archive = resolveFromRepository(mavenVersion);
        } else {
            archive = resolveFromRepository("org.apache.maven:apache-maven:zip:bin:" + mavenVersion);
        }

        installDirectory.mkdirs();
        if (!installDirectory.isDirectory()) {
            throw new IllegalStateException("Could not create Maven install directory " + installDirectory);
        }

        unarchiver.unarchive(archive, installDirectory);

        if (!mvn.isFile()) {
            throw new IllegalStateException("Unpacking of Maven distro failed");
        }
        mvn.setExecutable(true);

        return installDirectory;
    }
}
