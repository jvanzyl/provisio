package io.provis.jenkins.config.templates;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlMergerTest {
  
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

  @Test
  public void testAppendPath() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<a><test><foo><bar/></foo></test></a>");
    m.merge("<foo appendPath=\"a/test\"><baz/></foo>");
    assertEquals(XMLDECL + "<a><test><foo><bar/></foo><foo><baz/></foo></test></a>", m.finish());
  }

  @Test
  public void testMergePath() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<a><test><foo><bar/></foo></test></a>");
    m.merge("<foo mergePath=\"a/test/foo\" attr=\"bar\"><baz/></foo>");
    assertEquals(XMLDECL + "<a><test><foo><bar/><baz/></foo></test></a>", m.finish());
  }

  @Test
  public void testReplacePath() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<a><test><foo><bar/></foo></test></a>");
    m.merge("<foo replacePath=\"a/test/foo\" attr=\"bar\"><baz/></foo>");
    assertEquals(XMLDECL + "<a><test><foo attr=\"bar\"><baz/></foo></test></a>", m.finish());
  }

  @Test
  public void testReplacePath2() {
    XmlMerger m = new XmlMerger("test");
    m.merge("<a><test></test></a>");
    m.merge("<foo replacePath=\"a/test/foo\" attr=\"bar\"><baz/></foo>");
    assertEquals(XMLDECL + "<a><test><foo attr=\"bar\"><baz/></foo></test></a>", m.finish());
  }

}
