package io.provis.jenkins.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.provis.jenkins.config.templates.TemplateSource;

public class TemplateProcessor {

  private MustacheFactory mf;

  public TemplateProcessor() {
    mf = new DefaultMustacheFactory();
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
    Reader reader = new InputStreamReader(in);
    Writer writer = new OutputStreamWriter(out);
    Mustache mustache = mf.compile(reader, source.getName());
    
    Object[] sourceContexts = source.getContexts();
    if(sourceContexts != null && sourceContexts.length > 0) {
      List<Object> cl = new ArrayList<>();
      Collections.addAll(cl, sourceContexts);
      Collections.addAll(cl, contexts);
      contexts = cl.toArray(new Object[cl.size()]);
    }
    
    mustache.execute(writer, contexts).flush();
  }
}
