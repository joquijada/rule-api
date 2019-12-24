package com.exsoinn.ie.util;


import com.exsoinn.ie.test.TestData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by QuijadaJ on 4/21/2017.
 */
public class JsonUtilsTest {
    private static final String searchPath1 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.ORG_ID.REGN_NBR_ENTR.REGN_NBR_CD";
    private static final String searchPath2 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0]";
    private static final String searchPath3 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[0].NME_ENTR_VW";
    private static final String searchPath4 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.MTCH_RSLT.CAND_REF";
    private static final String searchPath5 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR.NME_ENTR_VW";
    private static final String searchPath6 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG";
    private static final String searchPath7 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_ADR.ADR_ENTR";
    private static final String searchPath8 = "VER_ORG.VERORG_MSGSV1.VERORG_TRN.VERORG_MSG.BUS_NME.NME_ENTR[1].NME_ENTR_VW";

    private static final String verOrgXml = TestData.verOrgXml;
    private static final String jsonStr = JsonUtils.convertXmlToJson(verOrgXml);

    private static final JsonParser jsonParser = new JsonParser();
    private static final JsonElement jsonElem =
            jsonParser.parse("{\"LANG_CD\":331,\"FRMT_CD\":0,\"NME_TEXT\":\"LIFE TIME WIRELESS INC\"}");

    @Test
    public void canConvertXmltoJson() {
        assertTrue(JsonUtils.convertXmlToJsonObject(verOrgXml).has("VER_ORG"));
    }

    @Test
    public void canSearchJson() {
        String key = "REGN_NBR_CD";
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath1, null, null, null);
        assertTrue(searchRes.containsKey(key) && "15336".equals(searchRes.get(key)));
    }


    @Test
    public void canSearchJsonWithArrayElementInSearchPath() {
        String key = "NME_ENTR";
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath2, null, null, null);
        assertTrue(searchRes.containsKey(key));
    }

    @Test
    public void canSearchJsonWithArrayElementInSearchPathThatIsNotLastNode() {
        String key = "NME_ENTR_VW";
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath3, null, null, null);
        assertTrue(searchRes.containsKey(key));
    }

    /**
     * For nodes that are not the last one in the search path, tests that they contain square brackets if
     * an array is expected in the JSON when that node is encountered. The reason for this check is that
     * even though currently only getting the first array entry is supported ("[0]"), in future might add support to
     * get something other than the first array entry.
     */
    @Test(expected = IllegalArgumentException.class)
    public void nonFinalNodeWithoutBracketsThrowsExceptionWhenArrayEncountered() {
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath5, null, null, null);
    }


    @Test
    public void canSearchJsonUsingFilter() {
        String key = "CAND_REF";
        Map<String, String> filter = new HashMap<>();
        String filterFld1 = "CAND_RNK";
        String filterVal1 = "1";
        String filterFld2 = "REGN_STAT_CD";
        String filterVal2 = "15201";
        filter.put(filterFld1, filterVal1);
        filter.put(filterFld2, filterVal2);
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath4, filter, null, null);
        assertTrue(searchRes.containsKey(key));
        JsonArray elem = (JsonArray) jsonParser.parse(searchRes.get(key));
        JsonObject jo = (JsonObject) elem.get(0);
        assertEquals(jo.get(filterFld1).toString(), filterVal1);
        assertEquals(jo.get(filterFld2).toString(), filterVal2);
    }


    @Test
    public void canSearchJsonUsingNestedFilter() {
        String key = "ADR_ENTR";
        Map<String, String> filter = new HashMap<>();
        String expectedElemName = "STD_STRG_VW";
        String expectedSubElemName = "STDN_APPL_CD";
        String filterFld1 = "STD_STRG_VW.STDN_APPL_CD";
        String filterVal1 = "13135";
        filter.put(filterFld1, filterVal1);
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath7, filter, null, null);
        assertTrue(searchRes.containsKey(key));
        JsonArray elem = (JsonArray) jsonParser.parse(searchRes.get(key));
        JsonObject jo = (JsonObject) elem.get(0);
        assertTrue(null != jo && jo.has(expectedElemName));
        JsonObject subJo = (JsonObject) jo.get(expectedElemName);
        assertTrue(null != subJo && filterVal1.equals(subJo.get(expectedSubElemName).getAsString()));

    }


    /**
     * For a node in the JSON, select only a sub-set of the elements contained therein. This can be useful for instance
     * when a rule is to be applied only to certain elements within an entity. Example: apply rule only to
     * city and state fields of mailing address of a business entity.
     */
    @Test
    public void selectSubsetOfElementsInFoundNode() {
        String key = "VERORG_MSG";
        Set<String> targetElems = new HashSet<>();
        String elemName1 = "ORG_ID";
        String elemName2 = "BUS_ADR";
        targetElems.add(elemName1);
        targetElems.add(elemName2);
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath6, null, targetElems, null);
        JsonObject elem = (JsonObject) jsonParser.parse(searchRes.get(key));
        assertNotNull(elem.get(elemName1));
        assertNotNull(elem.get(elemName2));
        assertEquals(2, elem.entrySet().size());
    }


    /**
     * Tests that the correct array entry is selected when the search path contains an array entry anywhere
     * that is *not* the final node. Final node array test will be added later elsewhere.
     */
    @Test
    public void selectCorrectArrayEntryInSearchPath() {
        Set<String> targetElems = new HashSet<>();
        String elemName1 = "STDN_APPL_CD";
        targetElems.add(elemName1);
        Map<String, String> searchRes = JsonUtils.findElement(jsonStr, searchPath8, null, targetElems, null);
        assertTrue(searchRes.size() == 1);
        assertTrue(searchRes.containsKey(elemName1));
        assertTrue(searchRes.get(elemName1).equals("13135"));
    }

    @Test
    public void testFilterUnwantedElements() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Set<String> pTargetElems = new HashSet<>();
        pTargetElems.add("NME_TEXT");

        Object[] objArgs = {jsonElem, pTargetElems};
        Method meth = invokePrivateMethods("filterUnwantedElements");
        JsonObject jObj = (JsonObject) meth.invoke(JsonUtils.class, objArgs);
        assertEquals("{\"NME_TEXT\":\"LIFE TIME WIRELESS INC\"}",jObj.toString());
    }

    @Test
    public void testShouldExcludeFromResults() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        HashMap<String,String> pTargetElems = new HashMap<String,String>();
        pTargetElems.put("NME_TEXT","LIFE TIME WIRELESS INC");

        Object[] objArgs = {jsonElem,pTargetElems};
        Method meth = invokePrivateMethods("shouldExcludeFromResults");
        boolean jObj = (boolean) meth.invoke(JsonUtils.class, objArgs);
        assertEquals(true, jObj);
    }

    private static Method invokePrivateMethods(String methodName) throws NoSuchMethodException {
        Method[] methods = JsonUtils.class.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equalsIgnoreCase(methodName)) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
