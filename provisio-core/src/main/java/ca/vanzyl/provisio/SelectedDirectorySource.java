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

import ca.vanzyl.provisio.archive.EntryContents;
import ca.vanzyl.provisio.archive.Source;
import ca.vanzyl.provisio.archive.SourceEntry;
import ca.vanzyl.provisio.model.Directory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codehaus.plexus.util.FileUtils;

/** Streams exactly the regular files selected by a Provisio file-set directory. */
final class SelectedDirectorySource implements Source {

    private final Path directory;
    private final List<String> relativePaths;

    static SelectedDirectorySource create(Directory selection) throws IOException {
        Path directory = Path.of(selection.getPath());
        List<String> paths = FileUtils.getFileNames(
                directory.toFile(), patterns(selection.getIncludes()), patterns(selection.getExcludes()), false);
        paths.sort(String::compareTo);
        if (selection.isFlatten()) {
            Set<String> names = new HashSet<>();
            for (String path : paths) {
                if (!names.add(Path.of(path).getFileName().toString())) {
                    return null;
                }
            }
        }
        return new SelectedDirectorySource(directory, paths);
    }

    private SelectedDirectorySource(Path directory, List<String> relativePaths) {
        this.directory = directory;
        this.relativePaths = new ArrayList<>(relativePaths);
    }

    @Override
    public void forEachEntry(EntryConsumer consumer) throws IOException {
        for (String relativePath : relativePaths) {
            Path file = directory.resolve(relativePath);
            consumer.accept(SourceEntry.file(
                    relativePath.replace('\\', '/'),
                    EntryContents.of(file),
                    mode(file),
                    Files.getLastModifiedTime(file).toMillis()));
        }
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public void close() throws IOException {}

    private static String patterns(List<String> patterns) {
        return patterns == null || patterns.isEmpty() ? null : String.join(",", patterns);
    }

    private static int mode(Path file) {
        try {
            int mode = 0;
            for (PosixFilePermission permission : Files.getPosixFilePermissions(file)) {
                switch (permission) {
                    case OWNER_READ:
                        mode |= 0400;
                        break;
                    case OWNER_WRITE:
                        mode |= 0200;
                        break;
                    case OWNER_EXECUTE:
                        mode |= 0100;
                        break;
                    case GROUP_READ:
                        mode |= 040;
                        break;
                    case GROUP_WRITE:
                        mode |= 020;
                        break;
                    case GROUP_EXECUTE:
                        mode |= 010;
                        break;
                    case OTHERS_READ:
                        mode |= 04;
                        break;
                    case OTHERS_WRITE:
                        mode |= 02;
                        break;
                    case OTHERS_EXECUTE:
                        mode |= 01;
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown POSIX permission " + permission);
                }
            }
            return mode;
        } catch (IOException | UnsupportedOperationException e) {
            return -1;
        }
    }
}
