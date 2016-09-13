package io.provis.jenkins.config.templates;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileTemplateSource implements TemplateSource {

  private File root;
  private String name;;

  public FileTemplateSource(File root, String name) {
    this.root = root;
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public InputStream openStream() throws IOException {
    File file = new File(root, name);
    return new FileInputStream(file);
  }

  @Override
  public void process(StreamProcessor processor, OutputStream out) throws IOException {
    try (InputStream in = openStream()) {
      processor.process(in, out);
    }
  }

  @Override
  public boolean exists() {
    return new File(root, name).isFile();
  }

  public String toString() {
    return "file:" + root + '/' + name;
  }
}
