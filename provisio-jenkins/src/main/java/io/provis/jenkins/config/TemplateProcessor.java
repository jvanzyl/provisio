package io.provis.jenkins.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.function.Function;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateFunction;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;

import de.pdark.decentxml.XMLUtils;
import io.provis.jenkins.config.templates.TemplateSource;

public class TemplateProcessor {

  private MustacheFactory mf;

  public TemplateProcessor() {
    mf = new DefaultMustacheFactory() {
      @Override
      public void encode(String value, Writer writer) {
        // use xmlescape instead of html
        try {
          writer.write(escape(value));
        } catch (IOException e) {
          throw new MustacheException("Failed to encode value: " + value);
        }
      }
    };
  }
  
  private String escape(String value) {
    return XMLUtils.escapeXMLText(value);
  }

  public void fromTemplate(TemplateSource source, Object context, File outputDirectory) throws IOException {
    fromTemplate(source, context, outputDirectory, null);
  }

  public void fromTemplate(TemplateSource source, Object[] contexts, File outputDirectory) throws IOException {
    fromTemplate(source, contexts, outputDirectory, null);
  }

  public void fromTemplate(TemplateSource source, Object context, File outputDirectory, String outputName) throws IOException {
    fromTemplate(source, new Object[] {context}, outputDirectory, outputName);
  }

  public void fromTemplate(TemplateSource source, Object[] contexts, File outputDirectory, String outputName) throws IOException {
    File target;
    if (outputName != null) {
      // If an outputName is supplied then we will use that
      target = new File(outputDirectory, outputName);
    } else {
      // otherwise we'll use the templateName as the outputName
      target = new File(outputDirectory, source.getName());
    }

    try (OutputStream out = new FileOutputStream(target)) {
      source.process((i, o) -> process(source, contexts, i, o), out);
    }
  }

  private void process(TemplateSource source, Object[] contexts, InputStream in, OutputStream out) throws IOException {
    process(source.getName(), in, out, arraySum(source.getContexts(), contexts));
  }
  
  public void process(String name, InputStream in, OutputStream out, Object[] contexts) throws IOException {
    Reader reader = new InputStreamReader(in);
    Writer writer = new OutputStreamWriter(out);
    
    process(name, reader, writer, contexts);
  }
  
  public void process(String name, Reader reader, Writer writer, Object[] contexts) throws IOException {
    Mustache mustache = mf.compile(reader, name);
    mustache.execute(writer, arraySum(ESCAPES, contexts)).flush();
  }
  
  public String process(String template, Object context) {
    return process(template, asArray(context));
  }
  
  public String process(String template, Object[] contexts) {
    StringWriter w = new StringWriter();
    StringReader r = new StringReader(template);
    try {
      process("<template>", r, w, contexts);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
    return w.toString();
  }
  
  private static Object[] arraySum(Object o1, Object o2) {
    Object[] a1 = asArray(o1);
    Object[] a2 = asArray(o2);
    
    if(a1 == null || a1.length == 0) {
      return a2;
    }
    if(a2 == null || a2.length == 0) {
      return a1;
    }
    
    Object[] a = new Object[a1.length + a2.length];
    System.arraycopy(a1, 0, a, 0, a1.length);
    System.arraycopy(a2, 0, a, a1.length, a2.length);
    return a;
  }
  
  private static Object[] asArray(Object o) {
    if(o == null) {
      return null;
    }
    return o.getClass().isArray() ? (Object[]) o : new Object[]{ o };
  }
  
  private static final String DO_ESCAPE_ATTR = "doEscapeAttr";
  
  private static final Map<String, Object> ESCAPES = ImmutableMap.<String, Object>builder()
    .put("attr", new AttrEscapeFunction())
    .put(DO_ESCAPE_ATTR, new AttrDoEscapeFunction())
    .build();
  
  private static class AttrEscapeFunction implements TemplateFunction {
    @Override
    public String apply(String t) {
      if(t.contains("{{")) {
        throw new IllegalStateException("{{#attr}} requires a plain text as template");
      }
      return "{{#" + DO_ESCAPE_ATTR + "}}{{&" + t + "}}{{/" + DO_ESCAPE_ATTR + "}}";
    }
  };
  
  private static class AttrDoEscapeFunction implements Function<String, String> {
    @Override
    public String apply(String t) {
      return t.replace("'", "&apos;").replace("\"", "&quot;");
    }
  }
  
}
