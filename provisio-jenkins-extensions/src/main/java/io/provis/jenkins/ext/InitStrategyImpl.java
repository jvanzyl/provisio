package io.provis.jenkins.ext;

import hudson.init.InitStrategy;
import org.jvnet.hudson.reactor.Task;
import org.kohsuke.MetaInfServices;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import hudson.PluginManager;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link InitStrategy} to control loading plugins.
 *
 * @since 0.1
 */
@MetaInfServices({InitStrategy.class})
public class InitStrategyImpl extends InitStrategy {
  private static final Logger log = LoggerFactory.getLogger(InitStrategyImpl.class);

  public InitStrategyImpl() {
    log.info("Using customized initialization strategy");
  }

  /**
   * Returns the list of *.jpi, *.hpi and *.hpl to expand and load.
   *
   * <p>
   * Normally we look at {@code $JENKINS_HOME/plugins/*.jpi} and *.hpi and *.hpl.
   *
   * @return
   *      never null but can be empty. The list can contain different versions of the same plugin,
   *      and when that happens, Jenkins will ignore all but the first one in the list.
   */
  public List<File> listPluginArchives(PluginManager pm) throws IOException {
    String jenkinsRoot = System.getProperty("JENKINS_ROOT");
    File pluginsDirectory = new File(jenkinsRoot, "plugins");
    List<File> r = new ArrayList<File>();

    // the ordering makes sure that during the debugging we get proper precedence among duplicates.
    // for example, while doing "mvn jpi:run" or "mvn hpi:run" on a plugin that's bundled with Jenkins, we want to the
    // *.jpl file to override the bundled jpi/hpi file.
    getBundledPluginsFromProperty(r);
    Iterator<File> it = r.iterator();
    while (it.hasNext()) {
      File f = it.next();
      if (new File(pluginsDirectory, f.getName().replace(".hpi", ".jpi") + ".pinned").isFile()) {
        // Cf. PluginManager.copyBundledPlugin, which is not called in this case.
        log.info("ignoring {} since this plugin is pinned", f);
        it.remove();
      }
    }

    // similarly, we prefer *.jpi over *.hpi
    listPluginFiles(pluginsDirectory, ".jpl", r); // linked plugin. for debugging.
    listPluginFiles(pluginsDirectory, ".hpl", r); // linked plugin. for debugging. (for backward compatibility)
    listPluginFiles(pluginsDirectory, ".jpi", r); // plugin jar file
    listPluginFiles(pluginsDirectory, ".hpi", r); // plugin jar file (for backward compatibility)

    log.info("Plugin archives:");
    for (File file : r) {
      log.info("  {}", file);
    }

    return r;
  }

  private void listPluginFiles(File pluginsDirectory, String extension, Collection<File> all) throws IOException {
    File[] files = pluginsDirectory.listFiles(new FilterByExtension(extension));
    if (files == null)
      throw new IOException("Jenkins is unable to create " + pluginsDirectory + "\nPerhaps its security privilege is insufficient");

    all.addAll(Arrays.asList(files));
  }

  /**
   * Lists up additional bundled plugins from the system property {@code hudson.bundled.plugins}.
   * Since 1.480 glob syntax is supported.
   * For use in the "mvn hudson-dev:run".
   * TODO: maven-hpi-plugin should inject its own InitStrategyImpl instead of having this in the core.
   */
  protected void getBundledPluginsFromProperty(final List<File> r) {
    String hplProperty = System.getProperty("hudson.bundled.plugins");
    if (hplProperty != null) {
      for (String hplLocation : hplProperty.split(",")) {
        File hpl = new File(hplLocation.trim());
        if (hpl.exists()) {
          r.add(hpl);
        } else if (hpl.getName().contains("*")) {
          try {
            new DirScanner.Glob(hpl.getName(), null).scan(hpl.getParentFile(), new FileVisitor() {
              @Override
              public void visit(File f, String relativePath) throws IOException {
                r.add(f);
              }
            });
          } catch (IOException x) {
            log.warn("could not expand {}", hplLocation, x);
          }
        } else {
          log.warn("bundled plugin {} does not exist", hplLocation);
        }
      }
    }
  }

  /**
   * Selectively skip some of the initialization tasks.
   * 
   * @return
   *      true to skip the execution.
   */
  public boolean skipInitTask(Task task) {
    return false;
  }

  private static class FilterByExtension implements FilenameFilter {
    private final List<String> extensions;

    public FilterByExtension(String... extensions) {
      this.extensions = Arrays.asList(extensions);
    }

    public boolean accept(File dir, String name) {
      for (String extension : extensions) {
        if (name.endsWith(extension))
          return true;
      }
      return false;
    }
  }
}

