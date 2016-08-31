package io.provis.jenkins.config.templates;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.provis.jenkins.config.TemplateProcessor;

public class TemplateProcessorTest {

  @Test
  public void testEscapes() {
    
    Map<String, String> data = new HashMap<>();
    data.put("value", "abc'def\"ghi&jkl<mno>pqr");
    
    assertEquals("abc&apos;def&quot;ghi&jkl<mno>pqr", new TemplateProcessor().process("{{#attr}}value{{/attr}}", data));
    assertEquals("abc'def\"ghi&amp;jkl&lt;mno&gt;pqr", new TemplateProcessor().process("{{value}}", data));
  }
}
