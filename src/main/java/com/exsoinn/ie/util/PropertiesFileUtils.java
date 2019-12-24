package com.exsoinn.ie.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Util class to cumulatively load properties from various source properties files. Each time
 * you invoke {@link PropertiesFileUtils#getInstance(String, boolean)}, the properties get loaded into the same
 * {@code Map}. So if earlier a property name had been loaded and a later invocation of {@link PropertiesFileUtils#getInstance(String, boolean)}
 * loads the same property, the earlier property value gets overridden! To obtain this cumulative set of properties,
 * invoke {@link this#asMap()}.
 *
 * For convenience, method {@link this#asMap(String)} exists if you want to get just the properties
 * loaded for a particular file. See that method for more details.
 *
 * This class was created mainly out of a need to support a layered approach to configurations in whatever application
 * wants to make use of this class.
 *
 * @author Jose Quijada
 */
public final class PropertiesFileUtils {
    private static final Map<String, PropertiesFileUtils> instances = new HashMap();
    private static final Map<String, String> properties = new HashMap<>();
    private final Map<String, String> perFileProperties = new HashMap<>();
    private static final Logger _LOGGER = LogManager.getLogger(PropertiesFileUtils.class);

    /**
     * Constructor initializes properties file
     * @param pFileName
     * @throws Exception
     */
    private PropertiesFileUtils(String pFileName, boolean pLoadFromClasspath) throws IOException {
        try  {
            InputStream is;

            if (pLoadFromClasspath) {
                ClassLoader classLoader = getClass().getClassLoader();
                is = classLoader.getResourceAsStream(pFileName);
            } else {
                is = new FileInputStream(pFileName);
            }

            Properties tempProps = new Properties();
            tempProps.load(is);

            properties.putAll(tempProps.entrySet().stream().collect(Collectors.toMap(
                            e -> e.getKey().toString(),
                            e -> e.getValue().toString())));
            perFileProperties.putAll(tempProps.entrySet().stream().collect(Collectors.toMap(
                    e -> e.getKey().toString(),
                    e -> e.getValue().toString())));
            _LOGGER.info("Properties file loaded: " + pFileName);
        } catch (Exception ex)  {
            _LOGGER.error("Error reading the properties file: " + pFileName, ex);
            throw ex;
        }
    }




    /**
     * Get the singleton instance of the property file
     * @param pFileName
     * @return instance
     * @throws Exception
     */
    public static PropertiesFileUtils getInstance(String pFileName, boolean pLoadFromClassPath) throws IOException {
       return getInstance(pFileName, pLoadFromClassPath, null);
    }


    /**
     * Does same thing as {@link PropertiesFileUtils#getInstance(String, boolean)}, and addition accepts a {@link Map} that can be used
     * to override any of the properties loaded from <code>pFileName</code> argument. Why would we need this functionality? For the usecase where the client/calling code
     * can perform some logic that warrants overriding one or more of the properties supplied in input <code>pFileName</code>. You see it is too late
     * to to try to override a property after an instance of {@link PropertiesFileUtils} is created, because the properties can't be modified after
     * these have been loaded. In other words the {@link PropertiesFileUtils#asMap()} and all overloaded such methods return unmodifiable maps via the use of the
     * {@link Collections#unmodifiableMap(Map)} Java Collections API method.
     *
     * @param pFileName - Name of file to load
     * @param pLoadFromClassPath - If true load pFileName from the resources, else from disk
     * @param pOverrideProps - A Map of string key/val pairs that overrides properties with the same name found in input file name pFileName
     * @return
     * @throws IOException
     */
    public static PropertiesFileUtils getInstance(String pFileName, boolean pLoadFromClassPath, Map<String, String> pOverrideProps) throws IOException {
        PropertiesFileUtils instance = instances.get(pFileName);
        if (instance == null) {
            instance = new PropertiesFileUtils(pFileName, pLoadFromClassPath);
            instances.put(pFileName, instance);
        }

        // Dynamically override any of the originalk properties supplied in the input file
        if (null != pOverrideProps) {
            properties.putAll(pOverrideProps);
        }
        return instance;
    }

    /**
     * Convert property file to map, returning an unmodifiable {@code Map} for more robust safety.
     *
     * @return
     */
    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(properties);
    }


    /**
     * Gives you only the properties loaded for a particular file name. If there's no such file name, null
     * gets returned, otherwise a Map that contains the properties associated with that file.
     * @param pFileName
     * @return
     */
    public Map<String, String> asMap(String pFileName) {
        PropertiesFileUtils pfu = instances.get(pFileName);
        if (null == pfu) {
            return null;
        }
        return Collections.unmodifiableMap(pfu.perFileProperties);
    }

}
