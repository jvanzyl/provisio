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

public class MakeDirectoryAction implements ProvisioningAction {

    private File runtimeDirectory;
    private String name;

    public void execute(ProvisioningContext context) {
        File directoryToMake = new File(runtimeDirectory, name);
        if (!directoryToMake.exists()) {
            if (!directoryToMake.mkdirs()) {
                throw new RuntimeException(String.format("Unable to create the directory %s", directoryToMake));
            }
        }
    }

    public File getRuntimeDirectory() {
        return runtimeDirectory;
    }

    public void setRuntimeDirectory(File runtimeDirectory) {
        this.runtimeDirectory = runtimeDirectory;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
