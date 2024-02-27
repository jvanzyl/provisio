package ca.vanzyl.provisio.archive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class MultiMap<K, V> {

    private final Map<K, List<V>> map = new LinkedHashMap<>();

    public void put(K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>());
        map.get(key).add(value);
    }

    public List<V> get(K key) {
        return map.get(key);
    }

    public Set<Map.Entry<K, List<V>>> entries() {
        return map.entrySet();
    }

    public List<V> values() {
        return map.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public int size() {
        return values().size();
    }
}
