package io.provis.jenkins.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class Template {

  private final File templateDirectory;
  
  public Template(File templateDirectory) {
    this.templateDirectory = templateDirectory;
  }
  
  public void fromTemplate(String templateName, Object context, File outputDirectory) throws IOException {
    fromTemplate(templateName, context, outputDirectory, null);
  }

  public void fromTemplate(String templateName, Object context, File outputDirectory, String outputName) throws IOException {
    File target;
    if (outputName != null) {
      // If an outputName is supplied then we will use that
      target = new File(outputDirectory, outputName);
    } else {
      // otherwise we'll use the templateName as the outputName
      target = new File(outputDirectory, templateName);
    }
    File templateFile = new File(templateDirectory, templateName);
    InputStream templateStream;
    if (templateFile.exists()) {
      // Try to use a local template first
      templateStream = new FileInputStream(templateFile);
    } else {
      // Otherwise try and pull it from the classpath
      templateStream = getClass().getClassLoader().getResourceAsStream(templateName);
    }
    if (templateStream != null) {
      // If we have a valid stream process through mustache
      MustacheFactory mf = new DefaultMustacheFactory();
      try (InputStream is = templateStream; Writer writer = new OutputStreamWriter(new FileOutputStream(target))) {
        Reader reader = new InputStreamReader(is);
        Mustache mustache = mf.compile(reader, "project");
        mustache.execute(writer, new Object[] {context}).flush();
      }
    }
  }  
}
