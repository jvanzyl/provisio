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
import java.util.TreeMap;
import java.util.function.Consumer;

public class Configuration implements Map<String, String> {

  private final Map<String, String> store;

  public Configuration(InputStream in) throws IOException {
    this();
    Properties props = new Properties();
    props.load(in);
    setData(props);
  }

  public Configuration(File file) {
    this();
    if (file != null && file.exists()) {
      Properties props = new Properties();
      try (InputStream in = new FileInputStream(file)) {
        props.load(in);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot load properties from " + file);
      }
      setData(props);
    }
  }

  public Configuration() {
    this.store = new TreeMap<>();
  }

  public Configuration(Properties props) {
    this();
    setData(props);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void setData(Properties props) {
    store.putAll((Map) props);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  public void ifNotEmpty(Consumer<Configuration> c) {
    if (!isEmpty()) {
      c.accept(this);
    }
  }

  public boolean has(String key) {
    return containsKey(key);
  }

  @Override
  public String get(Object key) {
    return store.get(key.toString());
  }

  public String get(Object key, String defValue) {
    String value = get(key);
    return value == null ? defValue : value;
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
      store.remove(key);
    } else {
      store.put(key, value.toString());
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
    
    for (Object k : store.keySet()) {
      String key = k.toString();
      if (key.startsWith(prefix)) {
        sub.set(key.substring(prefix.length()), store.get(key));
      }
    }
    return sub;
  }

  public Map<String, Configuration> partition() {
    Map<String, Configuration> partitions = new LinkedHashMap<>();
    for (Object k : store.keySet()) {
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
      partition.set(subKey, store.get(key));
    }
    return partitions;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<String> keySet() {
    return (Set) store.keySet();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Collection<String> values() {
    return (Collection) store.values();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<Map.Entry<String, String>> entrySet() {
    return (Set) store.entrySet();
  }

  @Override
  public int size() {
    return store.size();
  }

  @Override
  public boolean containsKey(Object key) {
    return store.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return store.containsValue(value);
  }

  @Override
  public String put(String key, String value) {
    return (String) store.put(key, value);
  }

  @Override
  public String remove(Object key) {
    return (String) store.remove(key);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    store.putAll(m);
  }

  @Override
  public void clear() {
    store.clear();
  }

}
