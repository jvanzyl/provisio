package io.provis.jenkins.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

public class Configuration implements Map<String, String> {

  private final Store store;

  public Configuration(InputStream in) throws IOException {
    this();
    load(in);
  }

  public Configuration(File file) {
    this();
    load(file);
  }

  public Configuration(Properties props) {
    this();
    setData(props);
  }

  public Configuration() {
    this(new MapStore());
  }

  private Configuration(Store store) {
    this.store = store;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void setData(Properties props) {
    putAll((Map) props);
  }

  public void loadSystem() {
    for (Map.Entry<String, String> e : System.getenv().entrySet()) {
      set("env." + e.getKey(), e.getValue());
    }
    setData(System.getProperties());
  }

  public void load(InputStream in) throws IOException {
    Properties props = new Properties();
    props.load(in);
    setData(props);
  }

  public void load(File file) {
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
    return getAndInterpolate(store.map(), store.key(key));
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
      remove(key);
    } else {
      put(key, value.toString());
    }
    return this;
  }

  public Set<String> keySet() {
    return store.keySet();
  }

  public Collection<String> values() {
    List<String> values = new ArrayList<>();
    for (String key : keySet()) {
      values.add(get(key));
    }
    return values;
  }

  public Set<Map.Entry<String, String>> entrySet() {
    Set<Map.Entry<String, String>> entries = new HashSet<>();
    for (String key : keySet()) {
      String value = get(key);
      entries.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
    }
    return entries;
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

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void putAll(Properties props) {
    putAll((Map) props);
  }

  @Override
  public void clear() {
    store.clear();
  }

  public Configuration subset(String prefix) {
    return new Configuration(new SubStore(store.map(), store.key(prefix)));
  }

  public Map<String, Configuration> partition() {
    Map<String, Configuration> subs = new TreeMap<>();
    for (String key : keySet()) {
      int dot = key.indexOf('.');
      String id;
      if (dot == -1) {
        id = key;
      } else {
        id = key.substring(0, dot);
      }
      if (!subs.containsKey(id)) {
        subs.put(id, subset(id));
      }
    }
    return subs;
  }

  private static String getAndInterpolate(Map<String, String> map, String key) {
    String value = map.get(key);
    if (value == null || !value.contains("${")) {
      return value;
    }
    Set<String> visited = new LinkedHashSet<>();
    visited.add(key);
    StringBuilder sb = new StringBuilder();
    interpolate(map, sb, value, visited);
    return sb.toString();
  }

  private static void interpolate(Map<String, String> map, StringBuilder sb, String value, Set<String> visited) {
    int l = value.length();
    int start = 0;
    while (true) {
      int idx = value.indexOf('$', start);
      if (idx + 1 >= l || value.charAt(idx + 1) != '{') {
        idx = -1;
      }
      if (idx == -1) {
        break;
      }

      int endIdx = value.indexOf('}', idx);
      if (endIdx == -1) {
        break;
      }

      String prop = value.substring(idx + 2, endIdx);
      if (!visited.add(prop)) {
        throw new IllegalStateException("Cyclic reference to `" + prop + "` in `" + value + "`, chain: " + visited);
      }
      sb.append(value, start, idx);
      if (!map.containsKey(prop)) {
        throw new IllegalStateException("Missing property `" + prop + "` in `" + value + "`");
      }
      String propValue = map.get(prop);
      if (propValue == null) {
        sb.append(value, idx, endIdx + 1);
      } else {
        interpolate(map, sb, propValue, visited);
      }
      visited.remove(prop);
      start = endIdx + 1;
    }
    if (start < l) {
      sb.append(value, start, l);
    }
  }

  @Override
  public String toString() {
    return store.toString();
  }

  private static interface Store {

    int size();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    Set<String> keySet();

    String get(Object key);

    String put(String key, String value);

    void putAll(Map<? extends String, ? extends String> m);

    String remove(Object key);

    void clear();

    String key(Object key);

    MapStore map();
  }

  private static class MapStore extends TreeMap<String, String> implements Store {
    private static final long serialVersionUID = 1L;

    @Override
    public String key(Object key) {
      return key.toString();
    }

    @Override
    public MapStore map() {
      return this;
    }
  }

  private static class SubStore implements Store {

    private MapStore map;
    private String prefix;

    public SubStore(MapStore map, String prefix) {
      this.map = map;
      this.prefix = prefix;
    }

    @Override
    public String key(Object key) {
      if (key == null || key.toString().trim().isEmpty()) {
        return prefix;
      }
      return prefix + "." + key;
    }

    @Override
    public MapStore map() {
      return map;
    }

    @Override
    public int size() {
      int l = 0;
      if (map.containsKey(prefix)) {
        l++;
      }
      String dotPref = prefix + ".";
      for (String key : map.keySet()) {
        if (key.startsWith(dotPref)) {
          l++;
        }
      }
      return l;
    }

    @Override
    public boolean containsKey(Object key) {
      return map.containsKey(key(key));
    }

    @Override
    public boolean containsValue(Object value) {
      return map.containsValue(value);
    }

    @Override
    public Set<String> keySet() {
      Set<String> keys = new TreeSet<>();
      if (map.containsKey(prefix)) {
        keys.add("");
      }
      String dotPref = prefix + ".";
      int l = dotPref.length();

      for (String key : map.keySet()) {
        if (key.startsWith(dotPref)) {
          keys.add(key.substring(l));
        }
      }
      return Collections.unmodifiableSet(keys);
    }

    @Override
    public String get(Object key) {
      return map.get(key(key));
    }

    @Override
    public String put(String key, String value) {
      return map.put(key(key), value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
      for (Map.Entry<? extends String, ? extends String> e : m.entrySet()) {
        put(e.getKey(), e.getValue());
      }
    }

    @Override
    public String remove(Object key) {
      return map.remove(key(key));
    }

    @Override
    public void clear() {
      map.remove(prefix);
      String dotPref = prefix + ".";
      for (String key : map.keySet()) {
        if (key.startsWith(dotPref)) {
          map.remove(key);
        }
      }
    }
  }
}
