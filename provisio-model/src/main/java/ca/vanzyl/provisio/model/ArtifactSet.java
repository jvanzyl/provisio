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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArtifactSet {

    // parse time
    private String directory;
    private String reference;
    private String from;
    private String providedBom;

    private final List<ProvisioArtifact> artifacts = new ArrayList<>();
    private final List<Resource> resources = new ArrayList<>();
    // children
    private final List<ArtifactSet> artifactSets = new ArrayList<>();
    private List<Exclusion> exclusions;

    // runtime
    private ArtifactSet parent;
    private File outputDirectory;
    private final Map<String, ProvisioArtifact> artifactMap = new LinkedHashMap<>();
    private Set<ProvisioArtifact> resolvedArtifacts = new HashSet<>();

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getProvidedBom() {
        return providedBom;
    }

    public void setProvidedBom(String providedBom) {
        this.providedBom = providedBom;
    }

    public void addArtifact(ProvisioArtifact artifact) {
        artifacts.add(artifact);
    }

    public List<ProvisioArtifact> getArtifacts() {
        return artifacts;
    }

    public void addResource(Resource resource) {
        resources.add(resource);
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void addArtifactSet(ArtifactSet artifactSet) {
        artifactSets.add(artifactSet);
    }

    public List<ArtifactSet> getArtifactSets() {
        return artifactSets;
    }

    // runtime

    public List<Exclusion> getExcludes() {
        return exclusions;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // maybe we can do this in the model

    public ArtifactSet getParent() {
        return parent;
    }

    public void setParent(ArtifactSet parent) {
        this.parent = parent;
    }

    public Set<ProvisioArtifact> getResolvedArtifacts() {
        return resolvedArtifacts;
    }

    public void setResolvedArtifacts(Set<ProvisioArtifact> resolvedArtifacts) {
        this.resolvedArtifacts = resolvedArtifacts;
    }

    public Map<String, ProvisioArtifact> getArtifactMap() {
        if (artifacts != null) {
            for (ProvisioArtifact artifact : artifacts) {
                artifactMap.put(artifact.getCoordinate(), artifact);
            }
        }
        return artifactMap;
    }

    @Override
    public String toString() {
        return "ArtifactSet [directory=" + directory + ", artifacts=" + artifacts + ", artifactSets=" + artifactSets
                + ", parent=" + parent + ", resolvedArtifacts=" + resolvedArtifacts + "]";
    }

    //
    // In order to set the parent references we use this technique:
    // http://xstream.codehaus.org/faq.html#Serialization_initialize_transient
    //
    private Object readResolve() {
        if (artifactSets != null) {
            for (ArtifactSet child : artifactSets) {
                child.setParent(this);
            }
        }
        return this;
    }
}
