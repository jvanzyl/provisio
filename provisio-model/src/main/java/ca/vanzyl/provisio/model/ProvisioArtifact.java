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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class ProvisioArtifact extends AbstractArtifact {

    private String name;
    private List<ProvisioningAction> actions;
    private Artifact delegate;
    private String coordinate;
    private List<String> exclusions;

    private String reference;

    public ProvisioArtifact(String coordinate) {
        this(coordinate, null);
    }

    public ProvisioArtifact(String coordinate, String name) {
        if (coordinate.indexOf(":") > 0) {
            this.delegate = new DefaultArtifact(coordinate);
            this.coordinate = coordinate;
        } else {
            this.reference = coordinate;
        }
        this.name = name;
    }

    public ProvisioArtifact(Artifact a) {
        this.delegate = a;
    }

    private ProvisioArtifact(
            String name,
            List<ProvisioningAction> actions,
            Artifact delegate,
            String coordinate,
            List<String> exclusions,
            String reference) {
        this.name = name;
        this.actions = actions;
        this.delegate = delegate;
        this.coordinate = coordinate;
        this.exclusions = exclusions;
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public String getCoordinate() {
        return coordinate;
    }

    public String getGA() {
        return getGroupId() + ":" + getArtifactId();
    }

    public String getGAV() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public String toVersionlessCoordinate() {
        StringBuffer sb = new StringBuffer()
                .append(getGroupId())
                .append(":")
                .append(getArtifactId())
                .append(":")
                .append(getExtension());
        if (getClassifier() != null && !getClassifier().isEmpty()) {
            sb.append(":").append(getClassifier());
        }
        return sb.toString();
    }

    public List<ProvisioningAction> getActions() {
        return actions;
    }

    public void addAction(ProvisioningAction action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }

    //
    //
    //

    @Override
    public String getGroupId() {
        return delegate.getGroupId();
    }

    @Override
    public String getArtifactId() {
        return delegate.getArtifactId();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public ProvisioArtifact setVersion(String version) {
        Artifact newArtifact = delegate.setVersion(version);
        if (this.delegate == newArtifact) {
            return this;
        }
        return new ProvisioArtifact(name, actions, newArtifact, coordinate, exclusions, reference);
    }

    @Override
    public String getBaseVersion() {
        return delegate.getBaseVersion();
    }

    @Override
    public boolean isSnapshot() {
        return delegate.isSnapshot();
    }

    @Override
    public String getClassifier() {
        return delegate.getClassifier();
    }

    @Override
    public String getExtension() {
        return delegate.getExtension();
    }

    @Override
    public File getFile() {
        return delegate.getFile();
    }

    @Override
    public ProvisioArtifact setFile(File file) {
        Artifact newArtifact = delegate.setFile(file);
        if (this.delegate == newArtifact) {
            return this;
        }
        return new ProvisioArtifact(name, actions, newArtifact, coordinate, exclusions, reference);
    }

    public Path getPath() {
        File file = getFile();
        return file != null ? file.toPath() : null;
    }

    public Artifact setPath(Path path) {
        Path current = getPath();
        if (Objects.equals(current, path)) {
            return this;
        }
        return setFile(path != null ? path.toFile() : null);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return delegate.getProperty(key, defaultValue);
    }

    @Override
    public Map<String, String> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public ProvisioArtifact setProperties(Map<String, String> properties) {
        Artifact newArtifact = delegate.setProperties(properties);
        if (this.delegate == newArtifact) {
            return this;
        }
        return new ProvisioArtifact(name, actions, newArtifact, coordinate, exclusions, reference);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ProvisioArtifact) {
            return delegate.equals(((ProvisioArtifact) obj).delegate);
        }

        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addExclusion(String exclude) {
        if (exclusions == null) {
            exclusions = new ArrayList<>();
        }
        exclusions.add(exclude);
    }

    public List<String> getExclusions() {
        return exclusions;
    }
}
