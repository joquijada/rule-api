package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;

/**
 * Parent class of all rule engines created.
 *
 * Child classes are forced to specify the path of where to find JavaScript files to load. Therefore,
 * the only constructor available expects as input parameter a {@link String} that denotes the path relative
 * to resources folder where this class can find JavaScript files to load.
 *
 * This class attempts to be thread safe by ensuring that every JavaScript file imported gets a different
 * {@link javax.naming.Binding} object <strong>per thread</strong>, thus making {@link javax.naming.Binding}
 * thread-confined (Goetz05 3.3.3 and 3.5.6). This ensures concurrent threads do not
 * step over each other, given that the {@link javax.naming.Binding} object is highly mutable. The {@link ScriptEngine} is
 * global and shared among all instances, because Oracle Java suggests it is thread safe, as per
 * <a href="https://blogs.oracle.com/nashorn/nashorn-multithreading-and-mt-safety">this article</a>. It is imperative
 * that this mechanism of not sharing {@link javax.naming.Binding} across threads does not get altered in any way, shape
 * or form.
 *
 *
 * @author QuijadaJ
 *
 */
@ThreadSafe
@Immutable
public abstract class AbstractRulesEngine implements RulesEngine {
    private static final Logger _LOGGER = Logger.getLogger(AbstractRulesEngine.class);
    private static final String ID_SCRIPT_ENGINE = "nashorn";
    private static final ScriptEngineManager engineManager = new ScriptEngineManager();

    /*
     * Something to keep an eye on. Here we're sharing Nashorn engine across all threads, based on Oracle's
     * suggestion that the ECMA engine is thread safe. If this becomes a problem, just make it thread local
     */
    private static final ScriptEngine engine = engineManager.getEngineByName(ID_SCRIPT_ENGINE);
    private final String moduleFolder;
    private final boolean loadFilesFromClassPath;
    private static final Map<String, Object> scriptCache = new ConcurrentHashMap<>();

    /*
     * This class Map provides a "hook" where child classes can add configuration properties
     * that are supported within the rule API.
     */
    private static final Map<String, String> rulesEngineConfigMap = new HashMap<>();

    /*
     * Creating a thread local cache of bindings, one per file, per thread, which essentially makes
     * each Bindings object thread-confined (each thread gets its own instance), as per Goetz06 Java Concurrency In
     * Practice, section 3.3. This is necessary because Nashorn Bindings objects are highly mutable, hence can't and should
     * not be shared between threads!!!
     * Moreover, it's OK if the tread goes away, thus losing this private "version" of bindinsCache. When a task comes,
     * if "bindingsCache" does not already contain a bindings object for the JavaScript file in question, a new
     * one will just simply get created. This comment was written in response to Goetz06 Java Concurrency In
     * Practice, 8.1, page # 168, search for "Tasks that use ThreadLocal".
     */
    private static final ThreadLocal<Map<String, Bindings>> bindingsCache =
            ThreadLocal.withInitial(() -> new HashMap<String, Bindings>());


    /*
     * Constructors
     */
    AbstractRulesEngine() {
        throw new UnsupportedOperationException("You must supply the path to find JavaScript files, use" +
                " AbstractRulesEngine(String pModuleFolder) instead!");
    }

    AbstractRulesEngine(String pModuleFolder, boolean pLoadFilesFromClasspath) {
        moduleFolder = pModuleFolder;
        loadFilesFromClassPath = pLoadFilesFromClasspath;
    }

    @Override
    public Object executeSelfEvaluatingFunction(String pFileName)
            throws ScriptException {
        try {
            return importJavaScriptFileIntoEngine(pFileName);
        } catch (FileNotFoundException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object executeFunctionByName(String pFileName, String pFunctionName, Object... pArgs)
            throws ScriptException {
        try {
            ScriptObjectMirror jsFile = (ScriptObjectMirror) importJavaScriptFileIntoEngine(pFileName, false, false);
            return jsFile.call(pFunctionName, pArgs);
        } catch (FileNotFoundException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Object executeAnonymousFunction(String pFileName, Object... pArgs)
            throws ScriptException {
        return executeFunctionByName(pFileName, null, pArgs);
    }

    /**
     *
     * @param pJsonStr
     * @return
     * @throws ScriptException
     * @throws NoSuchMethodException
     */
    public Object createJavaScriptJsonObjectFromString(String pJsonStr)
            throws ScriptException, NoSuchMethodException {
        Object json = getEngine().eval("JSON");
        Invocable invocable = (Invocable) getEngine();

        Object jsonObj = invocable.invokeMethod(json, "parse", pJsonStr);

        return jsonObj;
    }

    public String convertJsonObjectToString(Object pJson) throws ScriptException, NoSuchMethodException {
        Object json = getEngine().eval("JSON");
        Invocable invocable = (Invocable) getEngine();

        Object jsonStr = invocable.invokeMethod(json, "stringify", pJson);

        return jsonStr.toString();
    }



    /**
     * Same as <code>importJavaScriptFileIntoEngine(String pFileName, boolean pUseEngineBindings)</code>, but
     * this signature does *not* bind the imported JavaScript file's functions, variables, etc...
     * to the current engine's (available via <code>getEngine()</code>) JavaScript "global" object, instead using
     * a separate {@link Bindings}' per file per thread.
     * @param pFileName
     * @return
     * @throws FileNotFoundException
     * @throws ScriptException
     */
    @Override
    public Object importJavaScriptFileIntoEngine(String pFileName)
            throws FileNotFoundException, ScriptException {
        return importJavaScriptFileIntoEngine(pFileName, false, false);
    }


    /**
     * Import JavaScript file, and optionally bind it to current engine's "global" object, or
     * create a new, separate JavaScript <code>Object</code>, per thread and bind it to it. The latter is useful when
     * you're importing multiple JavaScript's into your session (I.e. running JVM instance), and you want to ensure they
     * don't step over each other, in a thread safe manner, if they have defined functions/variables/objects with the same name.
     * @param pFileName - Name of JavaScript file to load, should exist in the folder path
     *   specified when constructor {@link AbstractRulesEngine#AbstractRulesEngine(String, boolean)} was invoked by child class.
     * @param pUseEngineBindings - If true, use the engine's scope to evaluate the script. Passing true here will not work
     *                           as expected in multi-threaded environment, because the JavaScript scope, which is mutable,
     *                           is shared across all threads, which is not good - many unpredictable and weird things will
     *                           happen. But if false is passed, then each JavaScript file gets evaluated in its own,
     *                           separate, thread-local {@link Bindings} object, meaning that no two threads will share
     *                           the same{@link Bindings}, thus making it thread-safe.
     * @param pAlwaysReload - If true, unconditionally reload the JavaScript file from disk. This will be much slower
     *                      than if you pass <code>false</code> instead, in which case file is evaluated only the first time it's
     *                      seen, and from there after it gets loaded from cache.
     * @return - The {@link Object} that represents the evaluated JavaScript file.
     * @throws FileNotFoundException - If the file does not exist
     * @throws ScriptException - If Nashorn had trouble parsing the JavaScript code in the file
     */
    private Object importJavaScriptFileIntoEngine(String pFileName,
                                                  boolean pUseEngineBindings,
                                                  boolean pAlwaysReload)
            throws FileNotFoundException, ScriptException {

        Bindings bindings;
        if (pUseEngineBindings) {
            bindings = getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        } else {
            /*
             * Variable bindingsCache is a ThreadLocal Map. Therefore no need to worry about synchronizing, because
             * each thread will get it's own bindingsCache, meaning there won't be any other thread trying
             * to concurrently read/update it.
             */
            bindings = bindingsCache.get().get(pFileName);
            if (null == bindings) {
                bindings = (ScriptObjectMirror) getEngine().eval("new Object()");
                Bindings engineScope = getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
                bindings.putAll(engineScope);
                /**
                 * Why is this not synchronized? Because the bindingsCache Map is thread local, effectively making
                 * {@link AbstractRulesEngine#bindingsCache} thread-confined (Goetz06, Java Concurrency in Practice,
                 * Section 3.3.3, Page #45) - hence no need to unnecessarily incur added penalty of synchronizing
                 */
                bindingsCache.get().put(pFileName, bindings);
            }
        }

        Object evaledScript;

        if (pAlwaysReload) {
            evaledScript = evaluateScript(pFileName, bindings);
        } else {
            evaledScript = scriptCache.get(pFileName);
            if (null == evaledScript) {
                evaledScript = evaluateScript(pFileName, bindings);
                Object evaledScriptFromCache = scriptCache.putIfAbsent(pFileName, evaledScript);
                evaledScript = (null == evaledScriptFromCache) ? evaledScript : evaledScriptFromCache;
            }
        }

        return evaledScript;
    }


    private Object evaluateScript(String pFileName, Bindings pBindings) throws ScriptException, FileNotFoundException {
        Object evaledScript;

        if (loadFilesFromClassPath) {
            evaledScript = getEngine().eval(loadFileAsResourceFromClasspath(buildFullFilePath(pFileName)), pBindings);
        } else {
            evaledScript = getEngine().eval(new BufferedReader(new FileReader(buildFullFilePath(pFileName))), pBindings);
        }

        return evaledScript;
    }



    protected static void logError(Exception pExc) {
        logError(pExc, null);
    }

    protected static void logError(String pMsg) {
        logError(null, pMsg);
    }

    protected static void logError(Exception pExc, String pMsg) {
        StringBuilder errMsg = new StringBuilder();

        errMsg.append(null != pExc ? pExc.toString() : "");
        if (!CommonUtils.stringIsBlank(pMsg)) {
            errMsg.append(" ");
            errMsg.append(pMsg);
        }

        _LOGGER.error(AbstractRule.prefixWithThreadName(errMsg.toString() + (null != pExc ? ". Stack trace is "
                + CommonUtils.extractStackTrace(pExc).toString() : "")));
    }

    protected static void logDebug(String pStr) {
        _LOGGER.debug(AbstractRule.prefixWithThreadName(pStr));
    }

    protected static void logInfo(String pStr) {
        _LOGGER.info(AbstractRule.prefixWithThreadName(pStr));
    }

    private String buildFullFilePath(String pFileName) {
        StringBuilder filePath = new StringBuilder();
        filePath.append(moduleFolder);
        filePath.append(pFileName);
        logDebug("Full file path is: " + filePath.toString());
        return filePath.toString();
    }

    /**
     *
     * @param pFilePath
     * @return
     */
    private File loadFile(String pFilePath) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(pFilePath).getFile());
        return file;
    }

    /**
     *
     * @param pFilePath
     * @return
     */
    private Reader loadFileAsResourceFromClasspath(String pFilePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(pFilePath);
        if (null == is) {
            throw new IllegalArgumentException("Could not find file " + pFilePath + ". Check that path exists.");
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    protected void addConfigurationProperties(Map<String, String> pConfigsMap) {
        rulesEngineConfigMap.putAll(pConfigsMap);
    }

    protected static long obtainTaskTimeoutSeconds() {
        String timeoutSecs = "60";
        if (rulesEngineConfigMap.containsKey(RuleConstants.CONFIG_PROP_TASK_TIMEOUT_SECONDS)) {
            timeoutSecs = AbstractRulesEngine.rulesEngineConfigMap.get(RuleConstants.CONFIG_PROP_TASK_TIMEOUT_SECONDS);
        }

        return Long.valueOf(timeoutSecs);
    }

    public static String fetchConfigurationPropertyValue(String pPropName) {
        return rulesEngineConfigMap.get(pPropName);
    }

    /**
     * To indicate if there should be parallelization at all. For now this is all or nothing: either we have
     * prallelization, or not. Later can add more granular rule/component specific parallelization flags.
     * TODO: All-or-nothing for now, change to be more granular later on.
     * @return
     */
    protected static boolean parallelizedRuleExecution() {
        boolean ret = false;
        if (rulesEngineConfigMap.containsKey(RuleConstants.CONFIG_PROP_PARALLEL_RULE_EXEC)) {
            ret = Boolean.valueOf(AbstractRulesEngine.rulesEngineConfigMap.get(RuleConstants.CONFIG_PROP_PARALLEL_RULE_EXEC));
        }

        return ret;
    }

    /*
     * Getters/Setters
     */
    protected ScriptEngine getEngine() {
        return engine;
    }
}
