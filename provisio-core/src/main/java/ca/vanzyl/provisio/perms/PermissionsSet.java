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
package ca.vanzyl.provisio.perms;

import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

@SuppressWarnings("AssignmentToCollectionOrArrayFieldFromParameter")
public final class PermissionsSet {
    private final Set<PosixFilePermission> toAdd;
    private final Set<PosixFilePermission> toRemove;

    PermissionsSet(final Set<PosixFilePermission> toAdd, final Set<PosixFilePermission> toRemove) {
        this.toAdd = toAdd;
        this.toRemove = toRemove;
    }

    public Set<PosixFilePermission> modify(final Set<PosixFilePermission> set) {
        final Set<PosixFilePermission> ret = EnumSet.copyOf(set);

        ret.removeAll(toRemove);
        ret.addAll(toAdd);
        return ret;
    }
}
