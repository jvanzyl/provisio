package io.provis.jenkins.config.templates;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XmlMergeTemplateSource implements TemplateSource {

  private List<TemplateSource> sources;
  private String name;

  public XmlMergeTemplateSource(List<TemplateSource> sources, String name) {
    this.sources = sources;
    this.name = name;
  }

  @Override
  public boolean exists() {
    for (TemplateSource s : sources) {
      if (s.exists()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getName() {
    return name;
  }
  
  @Override
  public Object[] getContexts() {
    List<Object> contexts = null;
    for(TemplateSource s: sources) {
      Object[] sc = s.getContexts();
      if(sc != null && sc.length > 0) {
        if(contexts == null) {
          contexts = new ArrayList<>();
        }
        Collections.addAll(contexts, sc);
      }
    }
    if(contexts == null) {
      return null;
    }
    return contexts.toArray(new Object[contexts.size()]);
  }
  
  @Override
  public void process(StreamProcessor processor, OutputStream out) throws IOException {
    XmlMerger merger = new XmlMerger(name);
    for (TemplateSource s : sources) {
      ByteArrayOutputStream sout = new ByteArrayOutputStream();
      s.process(processor, sout);
      merger.merge(new ByteArrayInputStream(sout.toByteArray()));
    }
    merger.finish(out);
  }

  public static XmlMergeTemplateSource merge(String name, TemplateSource base, TemplateSource append) {

    List<TemplateSource> sources;
    if (base instanceof XmlMergeTemplateSource) {
      sources = new ArrayList<>(((XmlMergeTemplateSource) base).sources);
    } else {
      sources = new ArrayList<>();
      if (base != null) {
        sources.add(base);
      }
    }

    sources.add(append);
    return new XmlMergeTemplateSource(sources, name);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder().append(name).append(" = ");
    boolean f = true;
    for(TemplateSource s: sources) {
      if(f) f = false;
      else sb.append(" + ");
      sb.append(s.toString());
    }
    return sb.toString();
  }
}
