package com.exsoinn.ie.util;

import java.util.*;

/**
 * Sets up a forwarding {@link Map} that disallows operations that alter its state. It is still the responsibility of subclasses
 * to ensure their classes can't be extended to truly ensure immutability (Effective Java Item#15), if strict immutability is the
 * requirement, because a malicious/careless client can extend your class, override the fields that mutate this object, and then
 * pass a reference to an instance of the child mutable class to a method that expects any object that's a child of your child
 * class.
 * To provide more defense in depth against mutability, when instantiating using constructor
 * {@link ForwardingImmutableMap#ForwardingImmutableMap(Map)}, the passed in {@link Map} is defensively copied to safeguard against
 * any clever trick to try to modify the original {@code Map} in an effort to break the invariants offered by this class.
 *
 *
 * Created by QuijadaJ on 5/18/2017.
 */
public class ForwardingImmutableMap<K, V> extends ForwardingMap<K, V> {

    /**
     * Takes input {@code Map} and defensively creates a new copy, in case careless or malicious client wants to
     * break the invariants that this class guarantees.
     * @param pMap - The input {@code Map} to turn into an {@code ForwardingImmutableMap}
     */
    public ForwardingImmutableMap(Map<K, V> pMap) {
        super(new HashMap<>(pMap));

    }

    @Override
    public V put(K pKey, V pValue) {
        throw new UnsupportedOperationException("Modifications not allowed");
    }

    @Override
    public V remove(Object pKey) {
        throw new UnsupportedOperationException("Modifications not allowed");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> pMap) {
        throw new UnsupportedOperationException("Modifications not allowed");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Modifications not allowed");
    }

    @Override
    public Set<K> keySet() {
        return new HashSet<>(super.keySet());
    }


    @Override
    public Collection<V> values() {
        return new ArrayList(super.values());
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new HashSet<>(super.entrySet());
    }

}
