package io.provis.jenkins.config.templates;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlTemplateMergerTest {
  
  private static final String XMLDECL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
  
  @Test
  public void testAppend() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<test><foo><bar/></foo></test>");
    m.merge("<test><foo><baz/></foo></test>");
    assertEquals(XMLDECL + "<test><foo><bar/></foo><foo><baz/></foo></test>", m.finish());
  }

  @Test
  public void testMerge() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<test><foo><bar/></foo></test>");
    m.merge("<test><foo merge=\"true\"><baz/></foo></test>");
    assertEquals(XMLDECL + "<test><foo><bar/><baz/></foo></test>", m.finish());
  }

  @Test
  public void testReplace() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<test><foo><bar/></foo></test>");
    m.merge("<test><foo merge=\"replace\"><baz/></foo></test>");
    assertEquals(XMLDECL + "<test><foo><baz/></foo></test>", m.finish());
  }

}
