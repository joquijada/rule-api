package com.exsoinn.ie.util;


import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by QuijadaJ on 6/29/2017.
 */
public class PropertiesFileUtilsTest {

    @Test
    public void propertyOverridingWorks() throws IOException {
        String folder = "com/exsoinn/ie/util/";
        String propsFileName = folder + "test.properties";
        String childPropsFileName = folder + "test.properties.local";
        Map<String, String> propsMap = new HashMap<>(PropertiesFileUtils.getInstance(propsFileName.toString(), true)
                .asMap());
        propsMap.putAll(PropertiesFileUtils.getInstance(childPropsFileName, true).asMap());
        assertEquals(propsMap.get("prop1"), "val1_child");
        assertEquals(propsMap.get("prop2"), "val2_child");
        assertEquals(propsMap.get("prop3"), "val3");
        assertEquals(propsMap.get("prop4"), "val4");
    }
}
