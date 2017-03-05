package io.provis.action.artifact.filter;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;

import io.tesla.proviso.archive.UnarchivingEntryProcessor;

public class MustacheFilteringProcessor implements UnarchivingEntryProcessor {

  private Map<String, String> variables;
  
  public MustacheFilteringProcessor(Map<String, String> variables) {
    this.variables = variables;
  }

  @Override
  public String processName(String name) {
    return name;
  }

  @Override
  public void processStream(String entryName, InputStream inputStream, OutputStream outputStream) throws IOException {
    Writer writer = new OutputStreamWriter(outputStream);
    MustacheFactory mf = new DefaultMustacheFactory();
    Mustache mustache = mf.compile(new InputStreamReader(inputStream), "provisio");
    mustache.execute(writer, variables);
    writer.flush();    
  }
}
