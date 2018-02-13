package io.provis.jenkins.config.templates;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TemplateList {

  private static final String JAR_PREFIX = "jar:file:";
  private static final String FILE_PREFIX = "file:";

  private List<TemplateSource> templates;

  public TemplateList(List<TemplateSource> templates) {
    this.templates = templates;
  }

  public List<TemplateSource> getTemplates() {
    return templates;
  }

  public <T> TemplateList multiply(Collection<T> values, String varName, BiFunction<String, T, String> nameConverter) {
    List<TemplateSource> newTemplates = new ArrayList<>();
    for (T obj : values) {
      for (TemplateSource s : templates) {
        newTemplates.add(s.forName(nameConverter.apply(s.getName(), obj)).withContext(varName, obj));
      }
    }
    return new TemplateList(newTemplates);
  }

  public static TemplateList list(File dir) {
    if (dir == null) {
      return null;
    }
    List<TemplateSource> templates = new ArrayList<>();

    listFileTemplates(dir, templates);

    return new TemplateList(templates);
  }

  public static TemplateList as(Class<?> clazz, String name, String as) {
    TemplateList tl = of(clazz, name);
    return new TemplateList(Collections.singletonList(tl.getTemplates().get(0).forName(as)));
  }

  public static TemplateList of(Class<?> clazz, String... names) {
    String root = clazz.getName();
    int dotIdx = root.lastIndexOf('.');
    if (dotIdx != -1) {
      root = root.substring(0, dotIdx);
    }

    return of(root.replace('.', '/'), clazz, names);
  }

  public static TemplateList of(String root, Class<?> clazz, String... names) {
    List<TemplateSource> templates = new ArrayList<>();
    if (templates != null) {
      for (String name : names) {
        templates.add(new ClasspathTemplateSource(clazz.getClassLoader(), root, name));
      }
    }
    return new TemplateList(templates);
  }

  public static TemplateList list(Class<?> clazz) {
    return list(clazz, "");
  }

  public static TemplateList list(Class<?> clazz, String root) {
    List<TemplateSource> templates = new ArrayList<>();

    String pack = "";
    String name = clazz.getName();
    int dotIdx = name.lastIndexOf('.');
    if (dotIdx != -1) {
      pack = name.substring(0, dotIdx).replace('.', '/');
      name = name.substring(dotIdx + 1);
    }

    if (root.startsWith("/")) {
      root = root.substring(1);
    } else if (root.isEmpty()) {
      root = pack;
    } else {
      root = pack + '/' + root;
    }

    String location = URLDecoder.decode(clazz.getResource(name + ".class").toString());
    if (location.startsWith(JAR_PREFIX)) {

      location = location.substring(JAR_PREFIX.length());
      int bIdx = location.indexOf('!');
      if (bIdx != -1) {
        location = location.substring(0, bIdx);
      }

      listJarTemplates(clazz.getClassLoader(), new File(location), root, templates);

    } else if (location.startsWith(FILE_PREFIX)) {

      location = location.substring(FILE_PREFIX.length());
      String relPath = pack + '/' + name + ".class";

      if (!location.endsWith(relPath)) {
        throw new IllegalStateException("Path to classfile " + location + " is not " + relPath);
      }

      String classRoot = location.substring(0, location.length() - relPath.length());
      File dir = new File(classRoot, root);
      listFileTemplates(dir, templates);

    } else {

      throw new IllegalStateException("Cannot handle class location " + location);

    }

    return new TemplateList(templates);
  }

  private static void listFileTemplates(File dir, List<TemplateSource> templates) {
    Path root = dir.getAbsoluteFile().toPath();
    try {
      Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String path = root.relativize(file).toString();
          if (!path.endsWith(".class")) {
            path = path.replace('\\', '/'); // convert to unix-style
            templates.add(new FileTemplateSource(dir, path));
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void listJarTemplates(ClassLoader loader, File file, String root, List<TemplateSource> templates) {

    try (JarFile jar = new JarFile(file)) {
      Enumeration<JarEntry> en = jar.entries();
      while (en.hasMoreElements()) {
        JarEntry e = en.nextElement();
        if (!e.isDirectory() && !e.getName().endsWith(".class")) {
          String path = e.getName();
          if (path.startsWith(root + "/")) {
            String name = path.substring(root.length() + 1);
            templates.add(new ClasspathTemplateSource(loader, root, name));
          }
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final String MERGEXML_SUFFIX = "-merge.xml";
  private static final String NOPROCESS_SUFFIX = ".noprocess";

  public static TemplateList combined(List<TemplateList> lists) {
    Map<String, TemplateSource> map = new LinkedHashMap<>();

    for (TemplateList l : lists) {
      for (TemplateSource s : l.getTemplates()) {
        String key = s.getName();

        // merge xml files
        if (key.endsWith(MERGEXML_SUFFIX)) {
          key = key.substring(0, key.length() - MERGEXML_SUFFIX.length()) + ".xml";
          TemplateSource base = map.get(key);
          s = XmlMergeTemplateSource.merge(key, base, s);
        }
        
        if (key.endsWith(NOPROCESS_SUFFIX)) {
          key = key.substring(0, key.length() - NOPROCESS_SUFFIX.length());
          s = s.noProcess(key);
        }

        map.put(key, s);
      }
    }

    return new TemplateList(new ArrayList<>(map.values()));
  }

}
