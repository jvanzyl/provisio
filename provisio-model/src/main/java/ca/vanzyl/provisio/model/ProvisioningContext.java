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

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;

public class ProvisioningContext {

    private final ProvisioningRequest request;
    private final ProvisioningResult result;
    private final HashSet<Path> laidDownFiles;

    public ProvisioningContext(ProvisioningRequest request, ProvisioningResult result) {
        this.request = request;
        this.result = result;
        this.laidDownFiles = new HashSet<>();
    }

    public ProvisioningRequest getRequest() {
        return request;
    }

    public ProvisioningResult getResult() {
        return result;
    }

    public boolean layDownFile(Path path) {
        requireNonNull(path);
        return laidDownFiles.add(path);
    }

    public boolean deleteLaidDownFile(Path path) {
        requireNonNull(path);
        return laidDownFiles.remove(path);
    }

    public int laidDownFiles() {
        return laidDownFiles.size();
    }

    public Map<String, String> getVariables() {
        return request.getVariables();
    }
}
