package io.provis.jenkins.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;

public class ConfigurationTest {

  @Test
  public void testSubset() {
    Configuration c = new Configuration()
      .set("x", "1")
      .set("x.y", "2")
      .set("x.y.z", "3")
      .set("y", "4")
      .set("y.z", "5");

    Configuration sub = c.subset("x");
    assertEquals(3, sub.size());
    assertEquals("1", sub.get(""));
    assertEquals("2", sub.get("y"));
    assertEquals("3", sub.get("y.z"));

    Configuration sub2 = sub.subset("y");
    assertEquals(2, sub2.size());
    assertEquals("2", sub2.get(""));
    assertEquals("3", sub2.get("z"));
    sub2.remove("z");
    assertEquals(1, sub2.size());
    assertEquals(4, c.size());
  }

  @Test
  public void testPartition() {
    Configuration c = new Configuration()
      .set("x", "1")
      .set("x.y", "2")
      .set("x.z.y", "3")
      .set("y", "4")
      .set("y.z", "5");

    Map<String, Configuration> p = c.partition();
    assertEquals(2, p.size());
    assertEquals(3, p.get("x").size());
    assertEquals(2, p.get("y").size());
    assertEquals("1", p.get("x").get(""));
    assertEquals("2", p.get("x").get("y"));
    assertEquals("3", p.get("x").get("z.y"));
    assertEquals("4", p.get("y").get(""));
    assertEquals("5", p.get("y").get("z"));

    Map<String, Configuration> p2 = p.get("x").partition();
    assertEquals(3, p2.size());
    assertEquals("1", p2.get("").get(""));
    assertEquals("2", p2.get("y").get(""));
    assertEquals("3", p2.get("z").get("y"));
  }

  @Test
  public void testInterpolation() {
    Configuration c = new Configuration()
      .set("x", "1${y}3")
      .set("x.y", "a${y.z}c")
      .set("y", "2")
      .set("y.z", "b");

    assertEquals("123", c.get("x"));
    assertEquals("abc", c.get("x.y"));

    Configuration sub = c.subset("x");
    assertEquals(2, sub.size());
    assertEquals("123", sub.get(""));
    assertEquals("abc", sub.get("y"));
    assertEquals("abc", sub.subset("y").get(""));

    Map<String, Configuration> p = c.partition();
    assertEquals(2, p.size());
    assertEquals("123", p.get("x").get(""));
    assertEquals("abc", p.get("x").get("y"));
  }

  @Test
  public void testInterpolationCycle() {
    Configuration c = new Configuration()
      .set("x", "1${y}3")
      .set("y", "a${z}c")
      .set("z", "${x}yz");

    try {
      fail(c.get("x"));
    } catch (IllegalStateException e) {
    }

    c = new Configuration()
      .set("x.y", "${y}")
      .set("y", "1");

    assertEquals("1", c.subset("x").get("y"));
  }

}
