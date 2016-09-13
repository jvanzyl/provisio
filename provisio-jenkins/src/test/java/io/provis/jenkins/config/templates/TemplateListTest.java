package io.provis.jenkins.config.templates;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;

public class TemplateListTest {
  
  @Test
  public void testFileClasspathTemplates() throws IOException {
    File root = new File("target/test-classes/io/provis/jenkins/config/templates").getAbsoluteFile();
    
    assertSourcesExist(TemplateList.list(root),
      new FileTemplateSource(root, "test1.txt"),
      new FileTemplateSource(root, "test2.txt"),
      new FileTemplateSource(root, "test3/test3.txt"));
  }
  
  @Test
  public void testLocalClasspathTemplates() throws IOException {
    File root = new File("target/test-classes/io/provis/jenkins/config/templates").getAbsoluteFile();
    
    assertSourcesExist(TemplateList.list(TemplateListTest.class),
      new FileTemplateSource(root, "test1.txt"),
      new FileTemplateSource(root, "test2.txt"),
      new FileTemplateSource(root, "test3/test3.txt"));
  }
  
  @Test
  public void testLocalClasspathTemplates2() throws IOException {
    ClassLoader cl = TemplateListTest.class.getClassLoader();
    String root = "io/provis/jenkins/config/templates";
    
    assertSourcesExist(TemplateList.of(TemplateListTest.class, "test1.txt", "test3/test3.txt"),
      new ClasspathTemplateSource(cl, root, "test1.txt"),
      new ClasspathTemplateSource(cl, root, "test3/test3.txt"));
  }
  
  @Test
  public void testJarClasspathTemplates() throws IOException {
    ClassLoader cl = Matcher.class.getClassLoader();
    String root = "org/hamcrest";
    
    assertSources(TemplateList.list(Matcher.class),
      new ClasspathTemplateSource(cl, root, "core/package.html"),
      new ClasspathTemplateSource(cl, root, "package.html"));
  }
  
  @Test
  public void testMultiply() throws IOException {
    File root = new File("target/test-classes/io/provis/jenkins/config/templates").getAbsoluteFile();
    
    List<String> col = Arrays.asList("abc", "def");
    
    Object[] c1 = new Object[]{ Collections.singletonMap("n", "abc") };
    Object[] c2 = new Object[]{ Collections.singletonMap("n", "def") };
    
    assertSources(TemplateList.list(root).multiply(col, "n", (s, n) -> n + "/" + s),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test1.txt"), "abc/test1.txt"), c1),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test2.txt"), "abc/test2.txt"), c1),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test3/test3.txt"), "abc/test3/test3.txt"), c1),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test1.txt"), "def/test1.txt"), c2),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test2.txt"), "def/test2.txt"), c2),
        new ContextTemplateSource(new RenamedTemplateSource(new FileTemplateSource(root, "test3/test3.txt"), "def/test3/test3.txt"), c2)
      );
  }
  
  @Test
  public void testCombined() throws IOException {
    File root1 = new File("target/test-classes/io/provis/jenkins/config/templates1").getAbsoluteFile();
    File root2 = new File("target/test-classes/io/provis/jenkins/config/templates2").getAbsoluteFile();
    File root3 = new File("target/test-classes/io/provis/jenkins/config/templates3").getAbsoluteFile();
    
    TemplateList combined = TemplateList.combined(Arrays.asList(
      new TemplateList(Arrays.asList(
        new FileTemplateSource(root1, "test1.txt"),
        new FileTemplateSource(root1, "test2.txt"),
        new FileTemplateSource(root1, "test-merge.xml")
        )),
      new TemplateList(Arrays.asList(
          new FileTemplateSource(root2, "test2.txt"),
          new FileTemplateSource(root2, "test3.txt"),
          new FileTemplateSource(root2, "test-merge.xml")
        )),
      new TemplateList(Arrays.asList(
          new FileTemplateSource(root3, "test-merge.xml")
        ))
      ));
    
    assertSources(combined,
        new FileTemplateSource(root1, "test1.txt"),
        new FileTemplateSource(root2, "test2.txt"),
        new XmlMergeTemplateSource(Arrays.asList(
          new FileTemplateSource(root1, "test-merge.xml"),
          new FileTemplateSource(root2, "test-merge.xml"),
          new FileTemplateSource(root3, "test-merge.xml")
        ), "test.xml"),
        new FileTemplateSource(root2, "test3.txt")
      );
  }
  
  private void assertSources(TemplateList actual, TemplateSource ... expected) throws IOException {
    assertSources(actual, Arrays.asList(expected), false);
  }
  
  private void assertSourcesExist(TemplateList actual, TemplateSource ... expected) throws IOException {
    assertSources(actual, Arrays.asList(expected), true);
  }
  
  private void assertSources(TemplateList actual, List<TemplateSource> expected, boolean exist) throws IOException {
    List<String> expectedNames = new ArrayList<>();
    for(TemplateSource s: expected) {
      expectedNames.add(s.toString());
    }
    
    List<String> actualNames = new ArrayList<>();
    for(TemplateSource s: actual.getTemplates()) {
      actualNames.add(s.toString());
    }
    Collections.sort(expectedNames);
    Collections.sort(actualNames);
    
    assertEquals(expectedNames, actualNames);
    
    if(exist) {
      for(TemplateSource s: actual.getTemplates()) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        s.process(out);
        assertEquals(s.getName(), out.toString("UTF-8"));
      }
    }
    
  }
}
