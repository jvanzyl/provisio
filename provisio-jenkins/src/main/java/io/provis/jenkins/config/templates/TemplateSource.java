package io.provis.jenkins.config.templates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

public interface TemplateSource {

  boolean exists();

  String getName();

  void process(StreamProcessor processor, OutputStream out) throws IOException;

  default void process(OutputStream out) throws IOException {
    process(new StreamProcessor() {
      @Override
      public void process(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int l;
        while ((l = in.read(buf)) > 0) {
          out.write(buf, 0, l);
        }
      }
    }, out);
  }

  default Object[] getContexts() {
    return null;
  }

  default TemplateSource forName(String name) {
    return new RenamedTemplateSource(this, name);
  }
  
  default TemplateSource noProcess(String name) {
    return new NoProcessTemplateSource(this, name);
  }


  default TemplateSource withContext(Object[] ctx) {
    if (ctx == null || ctx.length == 0) {
      return this;
    }
    return new ContextTemplateSource(this, ctx);
  }

  default TemplateSource withContext(String name, Object value) {
    return withContext(Collections.singletonMap(name, value));
  }

  default TemplateSource withContext(Object ctx) {
    return withContext(new Object[] {ctx});
  }

  public interface StreamProcessor {
    void process(InputStream in, OutputStream out) throws IOException;
  }

}
