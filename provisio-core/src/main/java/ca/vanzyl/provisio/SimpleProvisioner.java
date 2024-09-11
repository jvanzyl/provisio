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

import ca.vanzyl.provisio.archive.UnArchiver;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SimpleProvisioner {

    public static final String DEFAULT_REMOTE_REPO = "https://repo1.maven.org/maven2";
    public static final File DEFAULT_LOCAL_REPO = new File(System.getProperty("user.home"), ".m2/repository");

    protected final UnArchiver unarchiver;
    protected final File localRepository;
    protected final String remoteRepositoryUrl;

    public SimpleProvisioner() {
        this(DEFAULT_LOCAL_REPO, DEFAULT_REMOTE_REPO);
    }

    public SimpleProvisioner(File localRepository, String remoteRepository) {
        this.localRepository = localRepository;
        this.remoteRepositoryUrl = remoteRepository;
        this.unarchiver = UnArchiver.builder().useRoot(false).flatten(false).build();
    }

    protected File resolveFromRepository(String coordinate) throws IOException {
        return resolveFromRepository(remoteRepositoryUrl, coordinate);
    }

    protected File resolveFromRepository(String repositoryUrl, String coordinate) throws IOException {
        String serverUrl;
        if (repositoryUrl == null) {
            serverUrl = remoteRepositoryUrl;
        } else {
            serverUrl = repositoryUrl;
        }
        String path = coordinateToPath(coordinate);
        String url = String.format("%s/%s", serverUrl, path);
        return resolveFromServer(url, coordinate);
    }

    protected File resolveFromServer(String archiveUrl, String coordinate) throws IOException {
        String path = coordinateToPath(coordinate);
        File file = new File(localRepository, path);
        if (file.exists()) {
            return file;
        }
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(archiveUrl).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Unexpected code " + response);
        }
        try (OutputStream os = new FileOutputStream(file);
                ResponseBody body = response.body()) {
            body.byteStream().transferTo(os);
        }
        return file;
    }

    protected String coordinateToPath(String coords) {
        Pattern p = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");
        Matcher m = p.matcher(coords);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad artifact coordinates " + coords
                    + ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
        }
        String groupId = m.group(1);
        String artifactId = m.group(2);
        String extension = get(m.group(4), "jar");
        String classifier = get(m.group(6), "");
        String version = m.group(7);
        return repositoryPathOf(groupId, artifactId, extension, classifier, version);
    }

    protected String get(String value, String defaultValue) {
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    protected String repositoryPathOf(
            String groupId, String artifactId, String extension, String classifier, String version) {
        //
        // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
        //
        // groupId/artifactId/version/artifactId-version[-classifier].extension
        //
        StringBuilder path = new StringBuilder()
                .append(groupId.replace('.', '/'))
                .append('/')
                .append(artifactId)
                .append('/')
                .append(version)
                .append('/')
                .append(artifactId)
                .append('-')
                .append(version);

        //
        // Aether's default classifier is "jar" so the only time we want to write out the classifier
        // is when there is a value that is not "jar".
        //
        if (classifier != null && !classifier.isEmpty() && !classifier.equals("jar")) {
            path.append("-").append(classifier);
        }
        path.append('.').append(extension);
        return path.toString();
    }
}
