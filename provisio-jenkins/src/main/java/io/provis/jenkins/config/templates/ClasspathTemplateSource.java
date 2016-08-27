package io.provis.jenkins.config.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClasspathTemplateSource implements TemplateSource {
  private ClassLoader loader;
  private String root;
  private String name;

  public ClasspathTemplateSource(ClassLoader loader, String root, String name) {
    this.loader = loader;
    this.root = root;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public InputStream openStream() throws IOException {
    InputStream in = loader.getResourceAsStream(root + '/' + name);
    if (in == null) {
      throw new IOException("Classpath resource " + root + '/' + name + " not found");
    }
    return in;
  }

  @Override
  public void process(StreamProcessor processor, OutputStream out) throws IOException {
    try (InputStream in = openStream()) {
      processor.process(in, out);
    }
  }

  @Override
  public boolean exists() {
    return loader.getResource(root + '/' + name) != null;
  }

  public String toString() {
    return "classpath:" + root + '/' + name;
  }
}
