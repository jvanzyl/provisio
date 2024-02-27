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

public class ProvisioArchive {

    private final java.io.File file;
    private final String extension;

    public ProvisioArchive(java.io.File file, String extension) {
        this.file = file;
        this.extension = extension;
    }

    public java.io.File file() {
        return file;
    }

    public String extension() {
        return extension;
    }
}
