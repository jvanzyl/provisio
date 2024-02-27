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
/**
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
package ca.vanzyl.provisio.action.runtime;

import ca.vanzyl.provisio.model.ProvisioningAction;
import ca.vanzyl.provisio.model.ProvisioningContext;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.FileUtils;

/**
 * Produces a POM that represents the dependencies of given runtime so that projects building
 * applications based on this runtime can easily access the dependencies.
 *
 * @author jvanzyl
 *
 */
public class PomAction implements ProvisioningAction {

    private String name;
    private String includes;
    private String excludes;
    private File runtimeDirectory;

    public void execute(ProvisioningContext context) {
        try {
            List<String> files = FileUtils.getFileNames(runtimeDirectory, includes, excludes, true);
            for (String file : files) {
                System.out.println(file);
            }
        } catch (IOException e) {
        }
    }

    private Dependency findDependencyFromFile(File file) {
        Dependency d = new Dependency();
        return d;
    }
}
