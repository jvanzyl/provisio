package io.provis.jenkins.config.templates;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContextTemplateSource implements TemplateSource {

  private TemplateSource delegate;
  private Object[] ctx;

  public ContextTemplateSource(TemplateSource delegate, Object[] ctx) {
    this.delegate = delegate;
    this.ctx = ctx;
  }

  @Override
  public boolean exists() {
    return delegate.exists();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void process(StreamProcessor processor, OutputStream out) throws IOException {
    delegate.process(processor, out);
  }

  @Override
  public Object[] getContexts() {
    Object[] dc = delegate.getContexts();
    if (dc != null && dc.length > 0) {
      List<Object> cl = new ArrayList<>();
      Collections.addAll(cl, ctx);
      Collections.addAll(cl, dc);
      return cl.toArray(new Object[cl.size()]);
    }
    return ctx;
  }

  @Override
  public TemplateSource withContext(Object[] ctx) {
    if (ctx == null || ctx.length == 0) {
      return this;
    }

    List<Object> cl = new ArrayList<>();
    Collections.addAll(cl, ctx);
    Collections.addAll(cl, this.ctx);
    return new ContextTemplateSource(delegate, new Object[cl.size()]);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(delegate.toString()).append(" contexts: [");

    boolean f = true;
    for (Object c : ctx) {
      if (f)
        f = false;
      else
        sb.append(", ");
      sb.append(c.toString());
    }
    sb.append("]");
    return sb.toString();
  }
}
