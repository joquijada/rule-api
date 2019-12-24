package com.exsoinn.ie.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import org.json.XML;
import java.util.*;


/**
 * Contains a collection of methods useful for dealing with JSON structures.
 *
 * Created by QuijadaJ on 4/20/2017.
 */
public class JsonUtils {
    private static final String SEARCH_PATH = "searchPath";
    /*
     * Suppresses default constructor, ensuring non-instantiability.
     */
    private JsonUtils() {

    }

    /**
     *
     * @param pXml
     * @return
     */
    public static String convertXmlToJson(String pXml) {
        JSONObject json = convertXmlToJsonObject(pXml);
        return json.toString();
    }

    public static JSONObject convertXmlToJsonObject(String pXml) {
        return XML.toJSONObject(pXml);
    }

    /**
     * Via externally configurable arguments, this method will search a JSON structure for the
     * elements specified. If found, the results are returned as name/value pairs for each
     * of the elements that the caller wanted to us to search for.
     * The idea here is to keep rules logic as separate as possible from the code. The only coupling/contract
     * is done via input arguments, to modify the behavior of this method.
     * The only pain point is that the input parameters must be properly configured before this method gets invoked,
     * but that's a small price to pay in comparison to the maintainability and re-usability that is gained by externally
     * configuring things in this way.
     *
     * @param pDataStr - The hierarchical data, in string format that this method will be searching upon.
     * @param pElemSearchPath - A dot (.) separated search path to drill down into in order to find results. If an array element will be
     *                    encountered somewhere in the search path, then the corresponding node should contain square
     *                    brackets like this: someElemName[0]. Also, when array element is encountered in the search path, only
     *                    the first array element is used. Current code does not support specifying something other than
     *                    the first entry ([0]) in the array element. If an array element is encountered yet the path
     *                    did not tell this method to expect an array at this point (by appending "[0]" to the
     *                    path node), IllegalArgumentException is thrown, *unless* the array element happens to be the
     *                    last node of the search path.
     * @param pElemFilter - A Map will be applied in all-or-nothing fashion to select JsonObject
     *                    type elements that match *all* of the name/value pairs contained in the Map. The key of
     *                    the Map corresponds to a member of the JsonObject, and the value corresponds to what the value
     *                    should be in order for that JsonObject to be included in the search results. As you can tell,
     *                    pElemFilter applies to JsonObject types only.
     * @param pTargetElems - This argument contains a {@link java.util.Set} of element names that should be included
     *                       in the results. It gets applied only to found elements of type JsonObject both inside and
     *                       outside an array in the last node of the search path.
     * @return - Map of name/value pairs found as per the {@param pElemPath}, where name is the element name found,
     *           and value is the value of such element. The
     *           values returned will all be in string format; this applies to non-primitive types as well like
     *           {@link com.google.gson.JsonObject}.
     *           Found elements which type is array are put in the Map is returned as a {@link java.util.List}
     * @throws IllegalArgumentException - Thrown if any of the input parameters are deemed incorrect.
     *
     * TODO: Add support to initial data structure given is an array
     * TODO: Refactor it to convey that the logic can be extended in future to handle different kinds of format
     * TODO: When reporting bad search path errors, since we're removing search nodes as we move along, it does not
     *   display full search path. Fix this by remembering original path.
     */
    public static Map<String, String> findElement(String pDataStr,
                                                  String pElemSearchPath,
                                                  Map<String, String> pElemFilter,
                                                  Set<String> pTargetElems,
                                                  Map<String, String> pExtraParams)
            throws IllegalArgumentException {
        JsonParser jsonParser = new JsonParser();
        final JsonElement json = jsonParser.parse(pDataStr);
        List<String> elemPath = parseElementSearchPath(pElemSearchPath);
        return findElement(json, elemPath, pElemFilter, pTargetElems, null, pExtraParams);
    }

    /**
     * Same as {@link #findElement(JsonElement, List, Map, Set, Map, Map)}, except that this method expects the JSON to search
     * to be an object of type {@link com.google.gson.JsonElement}, and caller can optionally pass a {@param pFoundElemVals}
     * to collect the search results.
     * @param pJson
     * @param pElemSearchPath
     * @param pElemFilter
     * @param pTargetElems
     * @param pFoundElemVals
     * @return
     * @throws IllegalArgumentException
     */
    public static Map<String, String> findElement(JsonElement pJson,
                                                  List<String> pElemSearchPath,
                                                  Map<String, String> pElemFilter,
                                                  Set<String> pTargetElems,
                                                  Map<String, String> pFoundElemVals,
                                                  Map<String, String> pExtraParams)
            throws IllegalArgumentException {
        if (null == pFoundElemVals) {
            pFoundElemVals = new HashMap<>(pTargetElems != null ? pTargetElems.size() : 0);
        }

        /*
         * At the very beginning, save the original element search path, because further
         * below it will get modified. We do this for error reporting purposes, to have the original
         * full search path available
         */
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }

        if (!pExtraParams.containsKey(SEARCH_PATH)) {
            pExtraParams.put(SEARCH_PATH, parseElementSearchPath(pElemSearchPath));
        }

        String curNodeInPath = pElemSearchPath.remove(0);
        String curNodeInPathNoBrackets = curNodeInPath;
        if (curNodeInPath.indexOf('[') >= 0) {
            curNodeInPathNoBrackets = curNodeInPath.substring(0, curNodeInPath.indexOf('['));
        }
        boolean atEndOfSearchPath = pElemSearchPath.isEmpty();
        JsonObject jo = null;
        if (pJson.isJsonObject()) {
            jo = (JsonObject) pJson;
        }

        /*
         * If "jo" is not NULL it means we're dealing with a JsonObject, as per check above. At this
         * point check if the current node in the search path we've been given exists in the current
         * JsonObject. If not, then it means the element will not be found, hence throw
         * IllegalArgumentException. The full search path given has to exist in order to return any results.
         */
        Set<Map.Entry<String, JsonElement>> joEntries = null;
        if (null != jo) {
            if (jo.has(curNodeInPathNoBrackets)) {
                joEntries = jo.entrySet();
            } else {
                throw new IllegalArgumentException("Did not find expected path node " + curNodeInPathNoBrackets
                        + " in the current part of the JSON currently being processed search. Check that the "
                        + "search path is correct: " + pExtraParams.get(SEARCH_PATH)
                        + ". JSON object is " + jo.toString());
            }
        }

        /*
         * If "joEntries" is not NULL, it means we're dealing with a JsonObject and the first element
         * in the search path has been found at this location of the passed in JSON to search. Why am I
         * structuring if() statements like this instead of nesting them? Makes code easier to read and
         * hence maintain, less nestedness.
         */
        if (null != joEntries) {
            for (Map.Entry<String, JsonElement> joEntry : joEntries) {
                /*
                 * If this pFoundElemVals is not empty, exit, no need to process further. It means we reached
                 * the last node in search path and found the goods. This was added here so that JVM does not
                 * continue iterating if there's more than one element in the JSON node that contains the node we're
                 * searching for.
                 */
                if (!pFoundElemVals.isEmpty()) {
                    return pFoundElemVals;
                }
                boolean shouldRecurse = false;
                JsonElement elemToRecurseInto = null;
                String curElemName = joEntry.getKey();

                if (!curNodeInPathNoBrackets.equals(curElemName)) {
                    continue;
                }
                JsonElement curElemVal = joEntry.getValue();
                /*
                 * If below evaluates to true, we're at the last node of our search path. Invoke helper
                 * method to add the elements to results for us.
                 */
                if (atEndOfSearchPath) {
                    processElement(curElemName, curElemVal, pElemFilter, pTargetElems, pFoundElemVals);
                } else if (curElemVal.isJsonArray()) {
                    if (curNodeInPath.indexOf('[') < 0) {
                        throw new IllegalArgumentException("Found an array element, yet the search path did not tell me"
                                + " to expect an array element here. The path node when error occurred was "
                                + curNodeInPath
                                + ", and the element found was " + curElemVal.toString()
                                + ". The search path was " + pExtraParams.get(SEARCH_PATH));
                    }

                    int aryIdx =
                            Integer.parseInt(
                                    curNodeInPath.substring(curNodeInPath.indexOf('[') + 1, curNodeInPath.indexOf(']')));
                    JsonElement curAryElemVal = curElemVal.getAsJsonArray().get(aryIdx);
                    if (curAryElemVal.isJsonObject()) {
                        shouldRecurse = true;
                        elemToRecurseInto = curAryElemVal;
                    }
                } else if (curElemVal.isJsonObject()) {
                    shouldRecurse = true;
                    elemToRecurseInto = curElemVal;
                }
                if (shouldRecurse) {
                    findElement(elemToRecurseInto, pElemSearchPath, pElemFilter, pTargetElems, pFoundElemVals, pExtraParams);
                }
            }
        }

        return pFoundElemVals;
    }


    /**
     * Takes care of "packaging" an element into the result Map. It is responsibility of the caller to invoke me only
     * when the last node in the JSON search path has been encountered.
     *
     * @param pElemName
     * @param pElem
     * @param pElemFilter
     * @param pTargetElems
     * @param pFoundElemVals
     * @throws IllegalArgumentException
     */
    private static void processElement(String pElemName,
                                       JsonElement pElem,
                                       Map<String,String> pElemFilter,
                                       Set<String> pTargetElems,
                                       Map<String, String> pFoundElemVals)
            throws IllegalArgumentException {

        Object elemVal = null;
        /*
         * Handle case when element in last node of search path is primitive or another JSON
         */
        if (pElem.isJsonPrimitive() || pElem.isJsonObject()) {
            /*
             * Hm, here the shouldExcludeFromResults() check might not be necessary. Why would the caller give
             * an element as last node in search path, and also give that element name in the pElemFilter Map?? In
             * other words, this might be a scenario that never happens, but leaving code here for now in case
             * there's something I'm missing.
             */
            if (pElem.isJsonObject() && shouldExcludeFromResults((JsonObject) pElem, pElemFilter)) {
                return;
            }

            if (pElem.isJsonPrimitive()) {
                elemVal = pElem.getAsString();
            } else {
                elemVal = pElem.toString();
            }

            //elemVal = pElem.getAsString();

            /*
             * The pTargetElems parameter applies only when results contain another JSON, apply here.
             */
            if (pElem.isJsonObject()) {
                elemVal = filterUnwantedElements((JsonObject) pElem, pTargetElems);
            }
        } else if (pElem.isJsonArray()) {
            Iterator<JsonElement> itJsonElem = pElem.getAsJsonArray().iterator();
            List<Object> elemValList = new ArrayList<>();
            itJsonElem.forEachRemaining(elem -> {

                /*
                 * In below if() expressions, if element is *not* a JsonObject, then the first part of OR
                 * will be true, and the rest will not get evaluated, as per JVM optimizations of if() statements. But
                 * if element *is* a JsonObject, then first part of OR evaluates to false, which allows us to safely
                 * assume that it is a JsonObject when calling shouldExcludeFromResults(), which
                 * expects its first argument type to be a JsonObject.
                 */
                if (!elem.isJsonObject() || !shouldExcludeFromResults((JsonObject) elem, pElemFilter)) {
                    if (elem.isJsonObject()) {
                        /*
                         * See comment further above regarding pTargetElems, same applies here.
                         */
                        elem = filterUnwantedElements((JsonObject) elem, pTargetElems);
                    }
                    elemValList.add(elem.toString());
                }
            });
            if (!elemValList.isEmpty()) {
                elemVal = elemValList;
            }
        } else {
            throw new IllegalArgumentException("One of the elements to search is of type not currently supported."
                    + "Element name/type is " + pElemName + "/" + pElem.getClass().getName());
        }

        if (null != elemVal) {
            /*
             * TODO: The below is effectively changing a List to a String, and storing it in Map, if the above found a
             *   JSON array. Re-visit this one more though is given on how to handle
             */
            pFoundElemVals.put(pElemName, elemVal.toString());
            deJsonizeWhenSingleValueFound(pFoundElemVals, pTargetElems);
        }
    }


    /*
     *
     * When the search results is a JSON object that contain just one name/value pair, and in case of arrays
     * only one JSON object with only one name/value pair, *and* the sole element name is in the pTargetElems Set, *and*
     * the value is a JSON primitive then the search results Map gets cleared, and this single name/value pair stored in results.
     * This was done to remove burden from client, to make it convenient for them where they can just blindly get
     * the key and value as-is from the search results map.
     */
    private static void deJsonizeWhenSingleValueFound(Map<String, String> pSearchRes,
                                               Set<String> pTargetElems) {
        Set<Map.Entry<String, String>> entries = pSearchRes.entrySet();
        if (entries.size() != 1) {
            return;
        }

        String val = entries.iterator().next().getValue();
        JsonParser jsonParser = new JsonParser();

        try {
            final JsonElement elem = jsonParser.parse(val);
            JsonObject jo = null;
            if (elem.isJsonArray() && elem.getAsJsonArray().size() == 1) {
                JsonElement aryElem = elem.getAsJsonArray().iterator().next();
                if (aryElem.isJsonObject()) {
                    jo = (JsonObject) aryElem;
                }
            } else if (elem.isJsonObject() && elem.getAsJsonObject().entrySet().size() == 1) {
                jo = (JsonObject) elem;
            } else {
                return;
            }

            if (null != jo) {
                Map.Entry<String, JsonElement> entry = jo.entrySet().iterator().next();
                if (null != pTargetElems && pTargetElems.contains(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                    pSearchRes.clear();
                    pSearchRes.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("A problem occurred while de-jsoninizing: " + e);
        }
    }

    /**
     * Gets rid of elements in the result set that the caller did not request via parameter
     * {@param pTargetElems}
     *
     * @param pElem
     * @param pTargetElems
     * @return
     */
    private static JsonObject filterUnwantedElements(JsonObject pElem, Set<String> pTargetElems) {
        if (null == pTargetElems) {
            return pElem;
        }

        Set<Map.Entry<String, JsonElement>> ents = pElem.entrySet();
        JsonObject jo = new JsonObject();
        ents.stream().filter(entry -> pTargetElems.contains(entry.getKey()))
                .forEach(entry -> jo.add(entry.getKey(), entry.getValue()));
        return jo;
    }

    private static boolean shouldExcludeFromResults(JsonObject pElem,
                                                    Map<String, String> pElemFilter)
            throws IllegalArgumentException {
        if (null == pElemFilter) {
            return false;
        }

        Set<Map.Entry<String, String>> filterEntries = pElemFilter.entrySet();
        for (Map.Entry<String, String> filterEntry : filterEntries) {
            boolean elemToFilterOnIsNested = filterEntry.getKey().indexOf('.') >= 0;

            if (elemToFilterOnIsNested) {
                /*
                 * Handles case when the value we want to filter on is buried one or more levels deeper off of . We leverage
                 * findElement(), which accepts a dot (.) separated element search path. Also, we support only filtering
                 * on primitive values, therefore assume that the found element will be a single name value pair.
                 * If the path of the filter element is not found, IllegalArgumentException is thrown.
                 */
                List<String> elemSearchPath = parseElementSearchPath(filterEntry.getKey());
                if (!pElem.has(elemSearchPath.get(0))) {
                    return true;
                }
                Map<String, String> filterElemFound = findElement(pElem, elemSearchPath, null, null, null, null);
                Set<Map.Entry<String, String>> entries = filterElemFound.entrySet();
                String filterVal = entries.iterator().next().getValue();
                if (null == filterVal) {
                    throw new IllegalArgumentException("The filter element value specified was not found off of this node: " +
                            filterEntry.getKey());
                }

                if (!filterVal.equals(filterEntry.getValue())) {
                    return true;
                }
            } else {
                JsonElement elem = pElem.get(filterEntry.getKey());
                if (null != elem && !elem.toString().equals(filterEntry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }


    /*
     * Turns the dot separated element search path into a List of strings.
     */
    private static List<String> parseElementSearchPath(String pElemSearchPath) {
        String[] nodes = pElemSearchPath.split("\\.");
        return CommonUtils.buildListFromArray(nodes);
    }


    /*
     * Turns the search path represented as a List into dot separated format
     */
    private static String parseElementSearchPath(List<String> pElemSearchPath) {
        StringBuilder sb = new StringBuilder();
        pElemSearchPath.forEach(e -> {
            sb.append(e);
            sb.append('.');
        });

        /*
         * Delete the last extraneous '.' added by above
         */
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
