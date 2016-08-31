package io.provis.jenkins.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

public class Configuration {

  private final Properties props;

  public Configuration(InputStream in) throws IOException {
    this();
    props.load(in);
  }

  public Configuration(File file) {
    this();
    if (file != null && file.exists()) {
      try (InputStream in = new FileInputStream(file)) {
        props.load(in);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot load properties from " + file);
      }
    }
  }

  public Configuration() {
    this.props = new Properties();
  }

  public Configuration(Properties props) {
    this.props = props;
  }

  public boolean isEmpty() {
    return props.size() == 0;
  }

  public void ifNotEmpty(Consumer<Configuration> c) {
    if (!isEmpty()) {
      c.accept(this);
    }
  }

  public boolean has(String key) {
    return props.containsKey(key);
  }

  public String get(String key) {
    return props.getProperty(key);
  }

  public String get(String key, String defValue) {
    return props.getProperty(key, defValue);
  }

  public int getInt(String key) {
    return getInt(key, -1);
  }

  public int getInt(String key, int defValue) {
    String v = get(key);
    if (v != null) {
      try {
        return Integer.parseInt(v);
      } catch (NumberFormatException e) {
      }
    }
    return defValue;
  }

  public boolean getBool(String key) {
    return getBool(key, false);
  }

  public boolean getBool(String key, boolean defValue) {
    String v = get(key);
    if (v != null) {
      return Boolean.parseBoolean(v);
    }
    return defValue;
  }

  public Configuration value(String key, Consumer<String> c) {
    if (has(key)) {
      c.accept(get(key));
    }
    return this;
  }

  public Configuration intValue(String key, Consumer<Integer> c) {
    if (has(key)) {
      c.accept(getInt(key));
    }
    return this;
  }

  public Configuration boolValue(String key, Consumer<Boolean> c) {
    if (has(key)) {
      c.accept(getBool(key));
    }
    return this;
  }

  public Configuration set(String key, Object value) {
    if (value == null) {
      props.remove(key);
    } else {
      props.setProperty(key, value.toString());
    }
    return this;
  }

  public Configuration subset(String prefix) {
    Configuration sub = new Configuration();
    if (!prefix.endsWith(".")) {
      prefix = prefix + ".";
    }
    
    String v = get(prefix.substring(0, prefix.length() - 1));
    if(v != null) {
      sub.set("", v);
    }
    
    for (Object k : props.keySet()) {
      String key = k.toString();
      if (key.startsWith(prefix)) {
        sub.set(key.substring(prefix.length()), props.getProperty(key));
      }
    }
    return sub;
  }

  public Map<String, Configuration> partition() {
    Map<String, Configuration> partitions = new LinkedHashMap<>();
    for (Object k : props.keySet()) {
      String key = k.toString();
      int dot = key.indexOf('.');
      String id;
      String subKey;
      if (dot == -1) {
        id = key;
        subKey = "";
      } else {
        id = key.substring(0, dot);
        subKey = key.substring(dot + 1);
      }
      Configuration partition = partitions.get(id);
      if (partition == null) {
        partitions.put(id, partition = new Configuration());
      }
      partition.set(subKey, props.getProperty(key));
    }
    return partitions;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<String> keySet() {
    return (Set) props.keySet();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<String> values() {
    return (Collection) props.values();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<Map.Entry<String, String>> entrySet() {
    return (Set) props.entrySet();
  }

}
