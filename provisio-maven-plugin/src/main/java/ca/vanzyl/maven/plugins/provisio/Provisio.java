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
package ca.vanzyl.maven.plugins.provisio;

import ca.vanzyl.provisio.Actions;
import ca.vanzyl.provisio.model.Runtime;
import ca.vanzyl.provisio.model.io.RuntimeReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.util.FileUtils;

@Named
@Singleton
public class Provisio {

    private final ArtifactHandlerManager artifactHandlerManager;
    private final MavenSession session;

    @Inject
    public Provisio(ArtifactHandlerManager artifactHandlerManager, MavenSession session) {
        this.artifactHandlerManager = artifactHandlerManager;
        this.session = session;
    }

    public List<Runtime> findDescriptors(File descriptorDirectory, MavenProject project) {
        List<Runtime> runtimes = new ArrayList<>();
        runtimes.addAll(findDescriptorsInFileSystem(descriptorDirectory, project));
        runtimes.addAll(findDescriptorsForPackagingTypeInExtensionRealms(project));
        return runtimes;
    }

    public List<Runtime> findDescriptorsInFileSystem(File descriptorDirectory, MavenProject project) {
        List<Runtime> runtimes = new ArrayList<>();
        if (descriptorDirectory.exists()) {
            try {
                List<File> descriptors = FileUtils.getFiles(descriptorDirectory, "*.xml", null);
                for (File descriptor : descriptors) {
                    Runtime runtime = parseDescriptor(new FileInputStream(descriptor), project);
                    runtimes.add(runtime);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format("Error parsing provisioning descriptors in %s.", descriptorDirectory), e);
            }
        }
        return runtimes;
    }

    public List<Runtime> findDescriptorsForPackagingTypeInExtensionRealms(MavenProject project) {
        List<Runtime> runtimes = new ArrayList<>();
        if (project.getClassRealm() != null) {
            Collection<ClassRealm> extensionRealms = project.getClassRealm().getImportRealms();
            if (extensionRealms != null) {
                for (ClassRealm extensionRealm : extensionRealms) {
                    String descriptorResourceLocation =
                            String.format("META-INF/provisio/%s.xml", project.getPackaging());
                    InputStream inputStream = extensionRealm.getResourceAsStream(descriptorResourceLocation);
                    if (inputStream != null) {
                        Runtime runtime = parseDescriptor(inputStream, project);
                        runtimes.add(runtime);
                    }
                }
            }
        }
        return runtimes;
    }

    public Runtime parseDescriptor(InputStream inputStream, MavenProject project) {
        RuntimeReader parser = new RuntimeReader(Actions.defaultActionDescriptors(), versionMap(project));
        Map<String, String> variables = new HashMap<>(getPropertiesWithSystemOverrides(project, session));
        variables.put("project.version", project.getVersion());
        variables.put("project.groupId", project.getGroupId());
        variables.put("project.artifactId", project.getArtifactId());
        variables.put("project.build.directory", project.getBuild().getDirectory());
        variables.put("project.basedir", project.getBasedir().getAbsolutePath());
        variables.put("basedir", project.getBasedir().getAbsolutePath());
        return parser.read(inputStream, variables);
    }

    private static Map<String, String> getPropertiesWithSystemOverrides(MavenProject project, MavenSession session) {
        Map<String, String> properties = new HashMap<>();
        project.getProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));
        session.getUserProperties().forEach((k, v) -> properties.put(String.valueOf(k), String.valueOf(v)));
        return properties;
    }

    //
    // The version map to use when versions are not specified for the artifacts in the assembly/runtime document.
    //
    private Map<String, String> versionMap(MavenProject project) {
        Map<String, String> versionMap = new HashMap<>();
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
            for (Dependency managedDependency :
                    project.getDependencyManagement().getDependencies()) {
                String versionlessCoordinate = toVersionlessCoordinate(managedDependency);
                versionMap.put(versionlessCoordinate, managedDependency.getVersion());
            }
        }
        //
        // Add a map entry for the project itself in the event that its not in dependency management so that users
        // don't have to put versions in the descriptor when including the project being built.
        //
        versionMap.put(toVersionlessCoordinate(project), project.getVersion());

        return versionMap;
    }

    public List<String> getManagedDependencies(MavenProject project) {
        List<String> managedDependencies = new ArrayList<>();
        DependencyManagement dependencyManagement = project.getDependencyManagement();
        if (dependencyManagement != null) {
            for (Dependency managedDependency :
                    project.getDependencyManagement().getDependencies()) {
                managedDependencies.add(toCoordinate(managedDependency));
            }
        }
        return managedDependencies;
    }

    public String toCoordinate(Dependency d) {
        StringBuilder sb = new StringBuilder()
                .append(d.getGroupId())
                .append(":")
                .append(d.getArtifactId())
                .append(":")
                .append(d.getType());
        if (d.getClassifier() != null && !d.getClassifier().isEmpty()) {
            sb.append(":").append(d.getClassifier());
        }
        sb.append(":").append(d.getVersion());
        return sb.toString();
    }

    public String toVersionlessCoordinate(Dependency d) {
        StringBuilder sb = new StringBuilder()
                .append(d.getGroupId())
                .append(":")
                .append(d.getArtifactId())
                .append(":")
                .append(d.getType());
        if (d.getClassifier() != null && !d.getClassifier().isEmpty()) {
            sb.append(":").append(d.getClassifier());
        }
        return sb.toString();
    }

    public String toVersionlessCoordinate(MavenProject project) {
        String extension = artifactHandlerManager
                .getArtifactHandler(project.getPackaging())
                .getExtension();
        return project.getGroupId() + ":" + project.getArtifactId() + ":" + extension;
    }
}
