package io.provis.jenkins.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
  
  public Configuration(File file) throws IOException {
    this();
    if (file != null && file.exists()) {
      try(InputStream in = new FileInputStream(file)) {
        props.load(in);
      }
    }
  }
  
  public Configuration() {
    this.props = new Properties();
  }
  
  public Configuration(Properties props) {
    this.props = props;
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
    if(v != null) {
      try {
        return Integer.parseInt(v);
      } catch(NumberFormatException e) {}
    }
    return defValue;
  }
  
  public boolean getBool(String key) {
    return getBool(key, false);
  }
  
  public boolean getBool(String key, boolean defValue) {
    String v = get(key);
    if(v != null) {
      return Boolean.parseBoolean(v);
    }
    return defValue;
  }
  
  public Configuration value(String key, Consumer<String> c) {
    String v = get(key);
    if(v != null) {
      c.accept(v);
    }
    return this;
  }
  
  public Configuration intValue(String key, Consumer<Integer> c) {
    String v = get(key);
    if(v != null) {
      c.accept(getInt(key));
    }
    return this;
  }
  
  public Configuration boolValue(String key, Consumer<Boolean> c) {
    String v = get(key);
    if(v != null) {
      c.accept(getBool(key));
    }
    return this;
  }
  
  public Configuration set(String key, Object value) {
    if(value == null) {
      props.remove(key);
    } else {
      props.setProperty(key, value.toString());
    }
    return this;
  }
  
  public Configuration subset(String prefix) {
    if(!prefix.endsWith(".")) {
      prefix = prefix + ".";
    }
    Configuration sub = new Configuration();
    for(Object k: props.keySet()) {
      String key = k.toString();
      if(key.startsWith(prefix)) {
        sub.set(key.substring(prefix.length()), props.getProperty(key));
      }
    }
    return sub;
  }
  
  public Map<String, Configuration> partition() {
    Map<String, Configuration> partitions = new LinkedHashMap<>();
    for(Object k: props.keySet()) {
      String key = k.toString();
      int dot = key.indexOf('.');
      String id;
      String subKey;
      if(dot == -1) {
        id = key;
        subKey = "";
      } else {
        id = key.substring(0, dot);
        subKey = key.substring(dot+1);
      }
      Configuration partition = partitions.get(id);
      if(partition == null) {
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
  public Set<Map.Entry<String, String>> entrySet() {
    return (Set) props.entrySet();
  }
}
