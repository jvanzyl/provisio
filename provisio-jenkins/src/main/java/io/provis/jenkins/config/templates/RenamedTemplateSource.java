package io.provis.jenkins.config.templates;

import java.io.IOException;
import java.io.OutputStream;

public class RenamedTemplateSource implements TemplateSource {

  private TemplateSource delegate;
  private String name;

  public RenamedTemplateSource(TemplateSource delegate, String name) {
    this.delegate = delegate;
    this.name = name;
  }

  @Override
  public boolean exists() {
    return delegate.exists();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void process(StreamProcessor processor, OutputStream out) throws IOException {
    delegate.process(processor, out);
  }

  @Override
  public TemplateSource forName(String name) {
    return delegate.forName(name);
  }

  public String toString() {
    return delegate.toString() + " -> " + name;
  }

  @Override
  public Object[] getContexts() {
    return delegate.getContexts();
  }

}
