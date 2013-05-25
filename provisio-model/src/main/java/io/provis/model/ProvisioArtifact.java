package io.provis.model;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.eclipse.aether.artifact.AbstractArtifact;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;

public class ProvisioArtifact extends AbstractArtifact {

  private Artifact delegate;
  private Map<String, Action> actionMap;
  private String coordinate;

  public ProvisioArtifact(String coordinate, List<Action> actions) {
    this(coordinate);
    this.coordinate = coordinate;
    setup(actions);
  }

  public ProvisioArtifact(String coordinate) {
    this.delegate = new DefaultArtifact(coordinate);
    this.coordinate = coordinate;
    setup(null);
  }

  protected ProvisioArtifact(Artifact a, List<Action> actions) {
    this.delegate = a;
    setup(actions);
  }

  public ProvisioArtifact(Artifact a) {
    this.delegate = a;
    setup(null);
  }

  private void setup(List<Action> actions) {
    actionMap = new LinkedHashMap<String, Action>();
    if (actions != null) {
      for (Action action : actions) {
        Named javaxNamed = action.getClass().getAnnotation(Named.class);
        actionMap.put(javaxNamed.value(), action);
      }
    }
  }

  public Action action(String name) {
    return actionMap.get(name);
  }

  public Collection<Action> getActions() {
    return actionMap.values();
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
                                                                                                                                                      
  //
  //
  //
  
  public String getGroupId() {
    return delegate.getGroupId();
  }

  public String getArtifactId() {
    return delegate.getArtifactId();
  }

  public String getVersion() {
    return delegate.getVersion();
  }

  public Artifact setVersion(String version) {
    delegate = delegate.setVersion(version);
    return this;
  }

  public String getBaseVersion() {
    return delegate.getBaseVersion();
  }

  public boolean isSnapshot() {
    return delegate.isSnapshot();
  }

  public String getClassifier() {
    return delegate.getClassifier();
  }

  public String getExtension() {
    return delegate.getExtension();
  }

  public File getFile() {
    return delegate.getFile();
  }

  public Artifact setFile(File file) {
    delegate = delegate.setFile(file);
    return this;
  }

  public String getProperty(String key, String defaultValue) {
    return delegate.getProperty(key, defaultValue);
  }

  public Map<String, String> getProperties() {
    return delegate.getProperties();
  }

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
}
