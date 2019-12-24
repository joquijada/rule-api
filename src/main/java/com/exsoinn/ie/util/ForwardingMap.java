package com.exsoinn.ie.util;

import java.util.*;

/**
 * Created by QuijadaJ on 5/17/2017.
 */
public class ForwardingMap<K, V> implements Map<K, V> {
    private final Map<K, V> m;


    public ForwardingMap(Map<K, V> pMap) {
        m = pMap;
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object pKey) {
        return m.containsKey(pKey);
    }

    @Override
    public boolean containsValue(Object pValue) {
        return m.containsValue(pValue);
    }

    @Override
    public V get(Object pKey) {
        return m.get(pKey);
    }

    @Override
    public V put(K pKey, V pValue) {
        return m.put(pKey, pValue);
    }

    @Override
    public V remove(Object pKey) {
        return m.remove(pKey);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> pMap) {
        m.putAll(pMap);
    }

    @Override
    public void clear() {
        m.clear();
    }

    @Override
    public Set<K> keySet() {
        return m.keySet();
    }

    @Override
    public Collection<V> values() {
        return m.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return m.entrySet();
    }

    @Override
    public String toString() {
        return m.toString();
    }
}
