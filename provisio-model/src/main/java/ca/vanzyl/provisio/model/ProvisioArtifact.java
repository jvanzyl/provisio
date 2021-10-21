/**
 * Copyright (C) 2015-2020 Jason van Zyl
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
import java.util.List;
import java.util.Map;

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

  public String getName() {
    return name;
  }

  public String getReference() {
    return reference;
  }

  public ProvisioArtifact(Artifact a) {
    this.delegate = a;
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
    StringBuffer sb = new StringBuffer().append(getGroupId()).append(":").append(getArtifactId()).append(":").append(getExtension());
    if (getClassifier() != null && getClassifier().isEmpty() == false) {
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
  public Artifact setVersion(String version) {
    delegate = delegate.setVersion(version);
    return this;
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
  public Artifact setFile(File file) {
    delegate = delegate.setFile(file);
    return this;
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
  public Artifact setProperties(Map<String, String> properties) {
    delegate = delegate.setProperties(properties);
    return this;
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
    if(exclusions == null) {
      exclusions = new ArrayList<>();
    }
    exclusions.add(exclude);
  }

  public List<String> getExclusions() {
    return exclusions;
  }
  
  
}
