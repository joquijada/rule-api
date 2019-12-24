package com.exsoinn.ie.rule;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable class to encapsulate inputs that would be given to a matrix like operation. To obtain an instance,
 * invoke factory method {@link MatrixDecisionInput#valueOf(String)}, giving input in a format that follows the
 * sample below:
 *
 *  key1=val1;key2=val2;key3=val3
 *
 *  You can also build a {@link MatrixDecisionInput} object from an existing {@link Map}, where the name/value
 *  pairs are the inputs to the matrix operation. Simply invoke factory method
 *  {@link MatrixDecisionInput#fromMap(Map)}.
 *
 * Created by QuijadaJ on 5/11/2017.
 */
public class MatrixDecisionInput implements Map<String, String> {
    private final Map<String, String> m = new HashMap<>();
    private static final String sampleFormat = "key1=val1;key2=val2;key3=val3";

    private MatrixDecisionInput(String pInput) {
        parseInput(pInput);
    }


    public static MatrixDecisionInput valueOf(String pInput)
            throws IllegalArgumentException {
        return new MatrixDecisionInput(pInput);
    }


    public static MatrixDecisionInput fromMap(Map<String, String> pInput) {
        if (null == pInput) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(pInput.entrySet().parallelStream().map(Map.Entry::toString)
                .collect(Collectors.joining(";", "", "")));
        return MatrixDecisionInput.valueOf(sb.toString());
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
    public String get(Object pKey) {
        return m.get(pKey);
    }

    @Override
    public String put(String pKey, String pValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object pKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> pMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return new HashSet<>(m.keySet());
    }

    @Override
    public Collection<String> values() {
        return new ArrayList<>(m.values());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new HashSet<>(m.entrySet());
    }

    @Override
    public String toString() {
        return m.toString();
    }


    private void parseInput(String pInput)
            throws IllegalArgumentException {
        String[] tokens = pInput.split(";");

        if (tokens.length == 0) {
            throw new IllegalArgumentException("Matrix input string " + pInput
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat);
        }

        try {
            Arrays.stream(tokens).forEach(t -> {
                String[] vals = t.split("=");
                if (vals.length==1) {m.put(vals[0], null);}
                else {m.put(vals[0], vals[1]);};
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Matrix input string " + pInput
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat, e);
        }
    }
}
