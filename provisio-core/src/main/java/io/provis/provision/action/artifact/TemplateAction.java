package io.provis.provision.action.artifact;

import io.provis.model.Action;
import io.provis.model.ProvisioContext;

import javax.inject.Named;

// use mustache

@Named("template")
public class TemplateAction implements Action {

  @Override
  public void execute(ProvisioContext context) throws Exception {
  }

  /*
  @Inject
  private Templater templater;
  private Map<String, Object> parameters;  
  private Artifact artifact;
  private File outputDirectory;
  
  public void execute(ProvisioContext context) {

    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs();
    }

    File archive = artifact.getFile();
    //
    // Create a template loader from the JAR file
    //
    templater.setTemplateLoader(new JarTemplateLoader(archive));

    try {
      JarFile jarFile = new JarFile(archive);
      //
      // Walk through all the entries in the JAR and treat them all as templates to be processed
      //
      Enumeration<JarEntry> e = jarFile.entries();
      for (; e.hasMoreElements();) {
        JarEntry entry = e.nextElement();
        //
        // We don't care about directories, and we'll ignore everything under META-INF
        //
        if (entry.isDirectory() || entry.getName().startsWith("META-INF")) {
          continue;
        }
        //
        // Make the directory where the template output will land if it doesn't exist
        //
        File outputFile = new File(outputDirectory, entry.getName());
        if (!outputFile.getParentFile().exists()) {
          outputFile.getParentFile().mkdirs();
        }
        Writer writer = new FileWriter(outputFile);
        templater.renderTemplate(entry.getName(), parameters, writer);
        IOUtil.close(writer);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  */
}