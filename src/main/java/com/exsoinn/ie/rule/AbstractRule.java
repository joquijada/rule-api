package com.exsoinn.ie.rule;

import com.exsoinn.ie.rule.data.DatabaseSource;
import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.ie.util.IdentifiedByName;
import com.exsoinn.ie.util.PropertiesFileUtils;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.*;
import net.jcip.annotations.Immutable;
import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * This abstract implementation of {@link Rule} interface is non-instantiable outside of its package. It can only be subclassed
 * by child classes inside of the same package. It exposes public static factory methods to create the various available
 * "flavors" of rule building blocks.
 *
 * <strong>WARNING: This class represents the only API publicly exposed to consumer code that wishes to build
 * rule logic. Please do not violate this contract when making changes to this or any other rule API
 * related class!!!</strong>
 *
 * The actual implementations returned are immutable, package private child classes of this class. This class is
 * instance controlled (Effective Java, Second Edition, Item #1 page 6), in that it guarantees that it will never dispense
 * duplicate rule objects, wherever possible. Instead, upon initial creation, the rule objects are cached using a suitable key for the rule
 * object "flavor", and the same object returned from cache on subsequent invocations. However, a suitable cache key might not be possible,
 * in which case caching will not be possible.
 *
 * Child classes of this class must
 * 1) Define a constructor that invokes this class' constructor, either {@link this#AbstractRule(String)} or
 * {@link this#AbstractRule(String, List)}. Among others this ensures that the rule object gets assigned an ID,
 * and given the unique name provided in the constructor (or a name will be auto-generated if NULL was given
 * as the rule name).
 * 2) Provide implementation for the {@link AbstractRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)} method, and provide a package private
 * constructor with the relevant input parameters for the rule flavor in question. The corresponding public static factory
 * method in this class can then instantiate the rule object at runtime using the input parameters of the constructor that
 * the child rule class provides. The idea is that the child classes provide as many constructors as necessary in order
 * to modify the behavior of {@link AbstractRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)}, which can be blindly called and expected
 * to function as requested as per the constructor used to instantiate the child class.
 *
 * This class is *conditionally* thread safe, except where otherwise noted for each method.
 *
 * Even in concurrent environment, no two duplicate objects of the same rule type will exist, as
 * mentioned in 2nd paragraph above. One caveat is that all of the factory methods below can create temporary duplicate objects
 * in a multi-threaded environment. This is an expected and harmless byproduct of the "double-check" idiom,
 * (Effective Java, 2nd Edition, Item #71, Page 283) which we leverage to do lazy initialization
 *  in conjunction with the use of atomic {@link ConcurrentHashMap#putIfAbsent(Object, Object)}.
 * To elaborate, each factory method below will first check in a racy manner if the rule object is contained in the
 * cache by invoking {@link ConcurrentHashMap#get(Object)}. If not
 * found, then it will proceed to invoke {@link ConcurrentHashMap#putIfAbsent(Object, Object)} synchronously, which actually checks
 * <strong>again</strong> if indeed a the rule object was put there by a different thread from the time of the first check to
 * the time of the second. If that other thread beat this one to it, then the "duplicate" created object simply gets discarded,
 * and the one created by the other thread that beat this one to it is the one returned. The main point to remember here
 * is that this is all transparent to the client!!! As far as client is concerned, they are guaranteed the rule objects will
 * be unique as per the cache key internally calculated for that rule flavor. Moreover, this behavior of transient duplicate
 * objects will/may happen only the first time when a rule object is requested by a given key. Subsequent invocations of the same
 * rule object will always be returned from cache, which improves performance big time after rule object is first initialized.
 * These details, which typically are left unspecified, are shared to explain any spikes in memory
 * and CPU usage that may be seen during performance testing in a highly multi-threaded environment.
 *
 *
 *
 * Created by QuijadaJ on 4/19/2017.
 */
@Immutable
public abstract class AbstractRule implements Rule, RuleIdentifier {
    private static final Logger _LOGGER = Logger.getLogger(AbstractRule.class);
    private static final AtomicLong idGenerator = new AtomicLong(0);
    private static final Map<String, Rule> ruleCache = new ConcurrentHashMap<>();
    static final String DEFAULT_FIELD_VAL = "DEFAULT";


    /*
     * This cache stores references to same rule objects as ruleCache, but keyed by either a given or
     * automatically computed rule name. This allows client to create a named rule and then to be able to retrieve
     * it by name as many times as necessary. A second cache might be redundant, but key lookup's are faster
     * than iterating over Map values to find a rule with a given name. Map ruleCache uses some other
     * key which is not the given rule name.
     */
    private static final Map<String, Rule> ruleByNameCache = new ConcurrentHashMap<>();
    private static final Map<String, DatabaseSource> databaseSourceByNameCache = new ConcurrentHashMap<>();
    static final Map<String, Context> listAssembledContextCache = new ConcurrentHashMap<>();
    static final Map<String, Map<String, Context>> listEntryContextLookupMap = new ConcurrentHashMap<>();


    private final long id;
    private final String uniqueName;
    private final Map<String, Rule> outputFieldMap;
    private static final String CACHE_KEY_TOKEN_SEPARATOR = "__";
    private static final Set<String> stashParameters = new HashSet<>();

    static {
        stashParameters.add(RuleConstants.NANO_START_TIME);
        stashParameters.add(RuleConstants.ARG_PARENT_CONTEXT);
        stashParameters.add(Context.IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR);
        stashParameters.add(Context.IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR);
    }


    /**
     * This generic Builder accepts two formal type parameters. The first if the type of object
     * that this builder produces, the second is the type returned by optional parameter setters. Why have the second one?
     * To be able to support inheritance of this Builder, whereby child classes can specify the builder type which is used
     * as the returned type of this parent class for optional parameter setters. W/o this approach, we'd get compilation
     * errors in calling code, when the calling code is invoking a mix of child class and parent class optional parameter
     * setters, because these two wouldn't return the same type if it wasn't for Java generics that came to the rescue
     * for us. Read <a href="https://stackoverflow.com/questions/17164375/subclassing-a-java-builder-class">here</a> for
     * a description of problem and solution
     * @param <T> - The type of object that this Builder produces when the {@link Builder#build()} method
     *           gets invoked. This works by erasure when child classes specify the type to build as the actual parameter,
     *           example "class ChildBuilder extends AbstractRule.Builder<<strong>SomeClass</strong>, ChildBuilder> {...}"
     * @param <U> - This is the child parametized type, which is the type that every optional parameter setter defined
     *           in this class will return. For example:
     *           "class ChildBuilder extends AbstractRule.Builder<SomeClass, <strong>ChildBuilder</strong>> {...}"
     */
    public abstract static class Builder<T, U extends Builder<T, U>> {
        // Required parameters
        private final String name;
        private String alternateSourceContextMapKey = null;


        // Optional parameters
        private List<String> outputFields = null;
        private boolean unwrapContextArrayResults = true;
        private QueryableDataRule selectionCriteriaSource = null;



        public Builder(String pName) {
            name = pName;
        }



        public U outputFields(List<String> pOutFlds) {
            if (null != pOutFlds) {
                outputFields = new ArrayList<>(pOutFlds);
            }
            return getThis();
        }

        /**
         * Child classes can use this optional parameter to designate the key in the extra params Map
         * passed in {@link ContextAugmentingRule#apply(Context, SearchPath, Filter, TargetElements, Map)} from where
         * to get the Context object to operate on when a rule component is given to get the value of the field to add.
         * @param pKey
         * @return - A {@link ContextAugmentingRule.Builder} object
         */
        public U alternateSourceContextMapKey(String pKey) {
            alternateSourceContextMapKey = pKey;
            return getThis();
        }

        public U unwrapContextArrayResults(boolean pFlag) {
            unwrapContextArrayResults = pFlag;
            return getThis();
        }


        public U selectionCriteriaSource(String pSrcName) throws RuleNameNotFoundException {
            QueryableDataRule qdr = (QueryableDataRule) lookupRuleByName(pSrcName);
            selectionCriteriaSource = qdr;
            return getThis();
        }



        /**
         * This helps avoid "unchecked warning", which would forces to cast to "T" in ach of the optional
         * parameter setters.
         * @return
         */
        abstract U getThis();
        public abstract T build();


        /*
         * Getters
         */
        public String getName() {
            return name;
        }
        public String getAlternateSourceContextMapKey() {
            return alternateSourceContextMapKey;
        }
        public List<String> getOutputFields() {
            return outputFields;
        }
        public boolean isUnwrapContextArrayResults() {
            return unwrapContextArrayResults;
        }
        public QueryableDataRule getSelectionCriteriaSource() {
            return selectionCriteriaSource;
        }

    }

    /*
     * Constructors
     */

    /**
     * @deprecated - Use {@link this#AbstractRule(Builder)} instead. Soon that constructor will
     *   replace this one
     * @param pUniqueName
     */
    AbstractRule(String pUniqueName) {
        this(pUniqueName, null);
    }


    /**
     * @deprecated - Use {@link this#AbstractRule(Builder)} instead. Soon that constructor will
     *   replace this one
     * @param pUniqueName
     * @param pOutFlds
     */
    AbstractRule(String pUniqueName, List<String> pOutFlds) {
        id = idGenerator.incrementAndGet();
        if (CommonUtils.stringIsBlank(pUniqueName)) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.getClass().getName());
            sb.append(id);
            uniqueName = sb.toString();
        } else {
            uniqueName = pUniqueName;
        }
        try {
            outputFieldMap = initOutFieldMap(pOutFlds);
        } catch (RuleNameNotFoundException e) {
            /*
             * It was late in the game and we didn't want all child classes to have to update
             * their method signatures. Besides rules are typically loaded during startup, hence
             * we'll know right away if there was bad data passed in.
             */
            throw new IllegalArgumentException(e);
        }
    }


    /**
     * The plan is for this to be the only constructor. Will slowly convert all child classes to using this one. Once
     * that's done, the "final" instance members that classes defined which are set from builder optional parameters,
     * can be moved to this class, and a getter provided. One example is "iteratorRule#unwrapContextArrayResults". This
     * process will take time. Finally the body of {@link this#AbstractRule(String, List)} will get moved here.
     * @param pBuilder
     */
    AbstractRule(Builder pBuilder) {
        this(pBuilder.getName(), pBuilder.getOutputFields());
    }

    /*
     * Public methods
     */
    public String name() {
        return uniqueName;
    }


    /**
     * This method is used for matrix like decisions. Given a {@link MatrixDecisionInput}, will search a
     * previously set up {@link QueryableDataRule} and output the result.
     * @param pMatrixRuleName - Name of {@link MatrixDecisionInput} that has the matrix decision data.
     * @param pInput - A {@link MatrixDecisionInput} that has all the fields necessary for the matrix to compute the
     *               the decision. The fields encapsulated by this {@link MatrixDecisionInput} object are assumed to
     *               exist in the underlying {@link QueryableDataRule}. This suggests that the developer must have a deep
     *               understanding of the {@link QueryableDataRule} against which she intends to make a decision.
     * @param pDecisionFieldName - The name of field in underlying {@link QueryableDataRule} that has the final
     *                           decision value that this method will return.
     * @param <T>
     * @return
     * @throws RuleException
     */
    public static <T extends MatrixDecisionInput> String matrixDecision(
            String pMatrixRuleName,
            T pInput,
            String pDecisionFieldName) throws RuleException {
        if (CommonUtils.stringIsBlank(pMatrixRuleName)) {
            throw new IllegalArgumentException("Matrix rule name cannot be blank!!!");
        }

        IQueryableDataRule r = (IQueryableDataRule) lookupRuleByName(pMatrixRuleName);

        if (null == r) {
            throw new RuleNameNotFoundException(pMatrixRuleName);
        }
        Filter f = Filter.fromMap(pInput);
        TargetElements t = TargetElements.valueOf(pDecisionFieldName);
        Context c = r.context();
        SearchPath sp = c.startSearchPath();
        Map<String, String> params = new HashMap<>(1);
        params.put(QueryableDataRule.FOUND_ELEM_VAL_IS_REGEX, "1");
        SearchResult sr = r.context().findElement(sp, f, t, params);


        /**
         * If matrix inputs failed to produce a decision, check if there's a default matrix decision, and if so
         * get that one.
         */
        if (null == sr || sr.isEmpty()) {
            /*
             * Get how many decision fields are contained in this matrix, and build that many filters
             * and have all the filter entries have value of 'DEFAULT'. If there is a row that
             * has defined all inputs as DEFAULT, then this filter will find it and we'll return that decision.
             */
            Map<String, String> defaultFilter =
                    f.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> DEFAULT_FIELD_VAL));
            sr = r.context().findElement(sp, Filter.fromMap(defaultFilter), t, null);
        }

        if (null == sr || sr.isEmpty() || sr.size() != 1 || !sr.get(sr.keySet().iterator().next()).isPrimitive()) {
            /*
             * Oh, oh, matrix didn't give any output, throw exception to alert user
             * of possible bad configuration
             */
            throw new MatrixCouldNotComputeDecisionException(r, f, sr);
        }

        return (null == sr.get(pDecisionFieldName)) ? null : sr.get(pDecisionFieldName).stringRepresentation();
    }


    /**
     * Takes as input the name of a {@link ConfigurableRuleExecutor} which when executed via its
     * {@link ConfigurableRuleExecutor#apply(Context, SearchPath, Filter, TargetElements, Map)}
     * method, trigger the
     * @param pMatrixName
     * @return
     */
    public static String configurableRuleReliantMatrixDecision(String pMatrixName) {
        return null;
    }

    /**
     *
     * @param pName
     * @return
     * @throws RuleNameNotFoundException
     */
    public static Rule lookupRuleByName(String pName) throws RuleNameNotFoundException {
        Rule r = ruleByNameCache.get(pName);
        if (null == r) {
            throw new RuleNameNotFoundException("Could not find rule name. Check spelling and/or that it "
                    + "was successfully loaded into memory before trying to retrieve it. Rule name given is " + pName);
        }

        return r;
    }


    public static DatabaseSource lookupDatabaseSourceByName(String pName) throws RuleNameNotFoundException {
        DatabaseSource dbSrc = databaseSourceByNameCache.get(pName);
        if (null == dbSrc) {
            throw new RuleNameNotFoundException("Could not find database source. Check spelling and/or that it "
                    + "was successfully loaded into memory before trying to retrieve it. Name given is " + pName);
        }

        return dbSrc;
    }




    static Context lookupContextByDataSourceName(String pDataSrcName) {
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            if (!(ruleEnt.getValue() instanceof IQueryableDataRule)) {
                continue;
            }
            QueryableDataRule qdr = (QueryableDataRule) ruleEnt.getValue();
            if (pDataSrcName.equals(qdr.dataSourceName())) {
                return qdr.context();
            }
        }

        return null;
    }


    /**
     * Same as {@link AbstractRule#createQueryableDataRule(Object, String, boolean)}, but with this signature, the field
     * values are *not* regarded/treated as regular expressions when evaluating search filter.
     * @param pData
     * @param pName
     * @return
     * @throws RuleException
     */
    public static Rule createQueryableDataRule(Object pData, String pName) throws RuleException {
        return createQueryableDataRule(pData, pName, false);
    }

    /**
     * Accepts the data object, which can be anything that {@link ContextFactory#obtainContext(Object)} can covert
     * into a Context object, and a unique name to be given to the rule.
     * Note that instances returned here are cached in a {@link ConcurrentHashMap} which key is the {@param pName}
     * passed in. This means that as long as different {@param pName} is passed, it's possible to have duplicate
     * {@link QueryableDataRule}s in cache. Unfortunately there's no suitable key which can be use to enforce
     * non-duplicate existence of {@link QueryableDataRule}s.
     * @param pData
     * @param pName
     * @param pFldValsAreRegEx - True if the field values should be treated as regular expressions when evaluating
     *                         search filter (if any)
     * @return
     * @throws RuleException
     */
    public static Rule createQueryableDataRule(Object pData, String pName, boolean pFldValsAreRegEx) throws RuleException {
        if (CommonUtils.stringIsBlank(pName)) {
            throw new RuleException("Name cannot be blank when requesting a QueryableDataRule");
        }

        Rule rule = new QueryableDataRule(pData, pName, pFldValsAreRegEx, null);

        /*
         * Convert to appropriate list component type if appropriate.
         */
        if (dataSourceIsListContainer(rule)) {
            rule = new QueryableLookupRule(pData, pName, pFldValsAreRegEx, null);
        }
        storeInRuleByNameCache(rule);

        return rule;
    }


    /**
     *
     * @param pRuleName
     * @param pCtx
     * @return
     * @throws RuleException
     */
    public static RuleExecutionResult executeConfigurableRule(String pRuleName, Context pCtx) throws RuleException {
        return executeConfigurableRule(pRuleName, pCtx, null);
    }

    public static RuleExecutionResult executeConfigurableRule(
            String pRuleName,
            Context pCtx,
            Map<String, String> pExtraParams) throws RuleException {
        String cacheKey = generateCacheKey(new String[] {ConfigurableRuleExecutor.GENERIC_CACHE_KEY});
        Rule rule = ruleCache.get(cacheKey);
        if (null == rule) {
            rule = new ConfigurableRuleExecutor(null);
            rule = uniqueRule(rule, cacheKey);
        }

        if (null == pExtraParams) {
            pExtraParams = new HashMap<>(1);
        }
        pExtraParams.put(RuleConstants.ARG_TARGET_RULE_NAME, pRuleName);
        return rule.apply(pCtx, null, null, null, pExtraParams);
    }


    public static RuleExecutionResult executeIterableRule(
            String pRuleName,
            Context pCtx,
            Map<String, String> pExtraParams)
            throws RuleException {
        Rule r = lookupRuleByName(pRuleName);
        if (!(r instanceof IteratorRule)) {
            throw new RuleException("Rule name " + pRuleName + " is not for a " + IteratorRule.class.getName()
                    + ", found a " + r.getClass().getName() + " instead.");
        }
        return r.apply(pCtx, null, null, null, pExtraParams);
    }





    /**
     *
     * @param pRuleFlowName
     * @param pContext
     * @return
     * @throws RuleException
     */
    public static RuleExecutionResult executeRuleFlow(String pRuleFlowName, Context pContext)
            throws RuleException {
        Rule rf = lookupRuleByName(pRuleFlowName);
        if (!(rf instanceof RuleFlow)) {
            throw new RuleException("Rule name " + pRuleFlowName + " is not for a rule flow, found a "
                    + rf.getClass().getName() + " instead.");
        }

        return rf.apply(pContext, null, null, null, null);
    }





    public static RuleExecutionResult evaluateRuleBasedMatrix(String pMatrixName, Context pCtx)
            throws RuleException {
        String cacheKey = generateCacheKey(new String[] {ConfigurableRuleBasedMatrixExecutor.GENERIC_CACHE_KEY});
        Rule rule = ruleCache.get(cacheKey);
        if (null == rule) {
            rule = new ConfigurableRuleBasedMatrixExecutor(null);
            rule = uniqueRule(rule, cacheKey);
        }

        Map<String, String> params = new HashMap<>(1);
        params.put(RuleConstants.RULE_NAME, pMatrixName);
        return rule.apply(pCtx, null, null, null, params);
    }


    public static RuleExecutionResult evaluateRegularExpression(String pStr, String pRegEx) throws RuleException {
        Map<String, String> params = new HashMap<>();
        Rule r = createGenericRegExRule();
        params.put(RuleConstants.ARG_REGEX_PATTERN, pRegEx);
        params.put(RuleConstants.ARG_REGEX_TARGET_VAL, pStr);
        return r.apply((Context) null, null, null, null, params);
    }


    public static String performStringChangingOperation(String pStr, StringHandlerRule.StringOperator pOp) throws RuleException {
        Map<String, String> params = new HashMap<>();
        Rule r = createStringHandlerRule(StringHandlerRule.Operator.NO_OP.toString(),
                pOp.toString());
        params.put(RuleConstants.ARG_LEFT_OPERAND, pStr);
        params.put(RuleConstants.ARG_RIGHT_OPERAND, null);
        RuleExecutionResult res = r.apply((Context) null, null, null, null, params);
        return res.evaluateResultAsString();
    }


    /**
     * Supplies all necessary information to build a rule object that can automatically query
     * for the set of rules that are applicable to the input data.
     * @param pData - See {@link AbstractRule#ruleSetBuilder(Object, String, String)}
     * @param pRuleName - See {@link AbstractRule#ruleSetBuilder(Object, String, String)}
     * @param pSelectCriteria - See {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder#ruleSetBuilder(Object, String, String)}
     *                        for a description of what this parameter is for.
     * @param pConfigString - Specially formatted string to pass various configuration options. The format is as follows:
     *                      "param1==val1&param2==val2&param3==val3....". Note that this use is deprecated. For a list
     *                      of all parameters available, see the description of option parameters in
     *                      {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder} class.
     * @param pOutFlds - See {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder#outputFields(List)} for
     *                 a description of what this parameter is for.
     * @return - the created {@link Rule} object.
     * @throws RuleException
     * @deprecated - Stop using this method. Instead use method {@link AbstractRule#ruleSetBuilder(Object, String, String)},
     *   to obtain a {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder} object which you can use to set optional
     *   fields, and then invoke {@link QueryableRuleConfigurationData.Builder#build()} to finally obtain the
     *   {@link QueryableRuleConfigurationData} object. Read documentation of {@link QueryableRuleConfigurationData.Builder#build()}
     *   for more details.
     */
    public static Rule createQueryableRuleConfigurationDataRule(
            Object pData,
            String pRuleName,
            Map<String, String> pSelectCriteria,
            String pConfigString,
            List<String> pOutFlds) throws RuleException {
        String[] tokens = pConfigString.split(Pattern.quote("&"));
        String leftOpFldName = null, opFldName = null, rightOpFldName = null,
                leftOpValOverride = null, resEvalOpt = null, succOutFldName = null, failOutFldName = null;
        boolean canExecInParallel = false, useTargetRuleOutput = false;
        for (String t : tokens) {
            String[] configNameValPair = t.split(Pattern.quote("=="));
            try {
                QueryableRuleConfigurationData.ConfigurationOption configOpt
                        = QueryableRuleConfigurationData.ConfigurationOption.fromString(configNameValPair[0]);
                switch(configOpt) {
                    case LEFT_OPERAND_FIELD_NAME:
                        leftOpFldName = configNameValPair[1];
                        break;
                    case OPERATOR_FIELD_NAME:
                        opFldName = configNameValPair[1];
                        break;
                    case RIGHT_OPERAND_FIELD_NAME:
                        rightOpFldName = configNameValPair[1];
                        break;
                    case LEFT_OPERAND_VALUE_OVERRIDE:
                        leftOpValOverride = configNameValPair[1];
                        break;
                    case RESULT_EVALUATION_OPTION:
                        resEvalOpt = configNameValPair[1];
                        break;
                    case SUCCESS_OUTPUT_FIELD_NAME:
                        succOutFldName = configNameValPair[1];
                        break;
                    case FAILURE_OUTPUT_FIELD_NAME:
                        failOutFldName = configNameValPair[1];
                        break;
                    case CAN_EXECUTE_IN_PARALLEL:
                        canExecInParallel = Boolean.valueOf(configNameValPair[1]);
                        break;
                    case USE_TARGET_RULE_OUTPUT:
                        useTargetRuleOutput = Boolean.valueOf(configNameValPair[1]);
                        break;
                }
            } catch (IllegalArgumentException e) {
                throw new RuleException(e.toString());
            }
        }

        return ruleSetBuilder(pData, pRuleName, resEvalOpt).leftOperandFieldName(leftOpFldName)
                .operatorFieldName(opFldName).rightOperandFieldName(rightOpFldName).ruleSetFilter(pSelectCriteria)
                .leftOperandValueOverride(leftOpValOverride).successOutputFieldName(succOutFldName)
                .failureOutputFieldName(failOutFldName).outputFields(pOutFlds).canExecInParallel(canExecInParallel)
                .useTargetRuleOutput(useTargetRuleOutput).build();
    }


    /**
     * This method returns a {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder} object which can be
     * used to create a {@link QueryableRuleConfigurationData} object. This is a perfect example
     * of the Builder pattern in action, as described by Effective Java BLOCH02, Item #2, pages 11 - 16. You can set
     * as many optional parameters as necessary ont he returned {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder}
     * before you decide to invoke the {@link QueryableRuleConfigurationData.Builder#build()} method. For a list
     * of available optional parameters, refer to documentation of {@link QueryableRuleConfigurationData.Builder}.
     * This is what client code might look like:
     *
     * <code>AbstractRule.ruleSetBuilder("rule config data in json format", "ruleName", "anyCanPass")
     *   .ruleSetFilter("rule set filter Map").build();</code>
     *
     * @param pRuleData - This is the data structure that holds the rule configuration data. It *must* be a format that
     *             {@link ContextFactory#obtainContext(Object)} can convert to a {@link Context} object. Read that API's
     *             documentation for more info on what formats are supported at present time.
     * @param pRuleName - Rule name, must be unique or {@link DuplicateCacheEntryException} gets thrown
     * @param pResEvalOpt - Dictates to the code how to handle scenario where multiple rule operations are returned for a given
     *                    rule configuration query. The string passed in *must* be a supported result evaluation option,
     *                    otherwise {@link IllegalArgumentException} gets thrown. Currently supported values for this argument
     *                    are: "allMustPass", "anyCanPass". The list may continue to grow in the future. This option
     *                    is all or nothing: it gets applied to rule configuration set in its entirety (I.e. there's
     *                    no way to apply a result evaluation to a set of records, and a different one to another set).
     * @return - A {@link com.exsoinn.ie.rule.QueryableRuleConfigurationData.Builder} on which you can call the various setter methods available to
     *  configure any desired optional options. When ready, you can then call the
     *  {@link QueryableRuleConfigurationData.Builder#build()} which will return a {@link QueryableRuleConfigurationData}
     *  instance. The {@link QueryableRuleConfigurationData.Builder#build()} method by the way takes care of loading the
     *  created {@link QueryableRuleConfigurationData} instance into {@link AbstractRule#ruleByNameCache}, which implies
     *  that if were to call {@link QueryableRuleConfigurationData.Builder#build()} again, then
     *  {@link DuplicateCacheEntryException} will get thrown.
     */
    public static QueryableRuleConfigurationData.Builder ruleSetBuilder(
            Object pRuleData,
            String pRuleName,
            String pResEvalOpt) {
        return new QueryableRuleConfigurationData.Builder(pRuleData, pRuleName, pResEvalOpt);
    }


    public static Rule createConfigurableRuleBasedMatrixRule(Object pData, String pRuleName) throws RuleException {
        if (CommonUtils.stringIsBlank(pRuleName)) {
            throw new RuleException("Name cannot be blank when requesting a ConfigurableRuleBasedMatrixRule");
        }
        Rule rule = new ConfigurableRuleBasedMatrixRule(pData, pRuleName, null);
        storeInRuleByNameCache(rule);
        return rule;
    }


    /**
     *
     * @param pRegEx
     * @return
     */
    public static Rule createRegExRule(String pRegEx) {
        return createRegExRuleWithFixedTargetString(pRegEx, null);
    }


    /**
     *
     * @param pRegEx
     * @param pTargetStr
     * @return
     */
    public static Rule createRegExRuleWithFixedTargetString(String pRegEx, String pTargetStr) {
        String[] keyAry = new String[] {pRegEx};
        if (!CommonUtils.stringIsBlank(pTargetStr)) {
            keyAry = new String[] {pRegEx, pTargetStr};
        }
        String cacheKey = generateCacheKey(keyAry);
        Rule rule = ruleCache.get(cacheKey);

        if (null == rule) {
            if (CommonUtils.stringIsBlank(pTargetStr)) {
                rule = new RegExRule(pRegEx);
            } else {
                rule = new RegExRule(pRegEx, null, pTargetStr);
            }
            rule = uniqueRule(rule, cacheKey);
        }

        return rule;
    }


    /**
     * Constructs a {@link OutputElementValueRule} using the given parameters
     * @param pName
     * @param pSelectionCriteria
     * @return
     * @throws RuleException
     */
    public static Rule createElementFinderRule(String pName, String pSelectionCriteria)
            throws RuleException {
        if (CommonUtils.stringIsBlank(pName)) {
            throw new RuleException("Name cannot be blank when requesting an OutputElementValueRule");
        }

        /**
         * Get a builder to construct rule object
         */
        Rule rule = elementFinderRuleBuilder(pName, pSelectionCriteria).build();

        return rule;
    }


    /**
     * Return a {@link OutputElementValueRule.Builder} object that can be used to build a
     * {@link OutputElementValueRule} object.
     * @param pName
     * @param pSelectionCriteria
     * @return
     */
    public static OutputElementValueRule.Builder elementFinderRuleBuilder(String pName, String pSelectionCriteria) {
        return new OutputElementValueRule.Builder(pName, pSelectionCriteria);
    }


    public static ExpressedRule.Builder expressedRuleBuilder(String pName, Object pData) {
        return new ExpressedRule.Builder(pName, pData);
    }


    public static IteratorRule.Builder iteratorRuleBuilder(String pName,
                                                           String pArySelCrit,
                                                           String pRuleToApply,
                                                           String pDestKey) {
        List<String> rules = new ArrayList<>();
        rules.add(pRuleToApply);
        return iteratorRuleBuilder(pName, pArySelCrit, rules, pDestKey);
    }

    public static IteratorRule.Builder iteratorRuleBuilder(String pName,
                                                           String pArySelCrit,
                                                           List<String> pRulesToApply,
                                                           String pDestKey) {
        return new IteratorRule.Builder(pName, pArySelCrit, pRulesToApply, pDestKey);
    }



    /**
     *
     * @param pName
     * @param pSelectionCriteria
     * @param pSelCriSrcName
     * @return
     * @throws RuleException
     * @deprecated - Stop using this method. Instead use construct below.
     *  First call {@link AbstractRule#elementFinderRuleBuilder(String, String)} to get a
     *  {@link OutputElementValueRule.Builder}, then do:
     *  myBuilder.selectionCriteriaSource(pSelCriSrcName).build()
     */
    public static Rule createElementFinderRuleUsingSelectionCriteriaSource(
            String pName,
            String pSelectionCriteria,
            String pSelCriSrcName) throws RuleException {
        if (CommonUtils.stringIsBlank(pName)) {
            throw new RuleException("Name cannot be blank when requesting an OutputElementValueRule");
        }

        return elementFinderRuleBuilder(pName, pSelectionCriteria).selectionCriteriaSource(pSelCriSrcName).build();
    }


    /**
     *
     * @param pName - See {@link AbstractRule#ruleOutputJoinerBuilder(String, List)}
     * @param pRuleNames - See {@link AbstractRule#ruleOutputJoinerBuilder(String, List)}
     * @param pDelim - See {@link AbstractRule#ruleOutputJoinerBuilder(String, List)}
     * @return
     * @throws RuleException
     * @deprecated - Stop using this method. Instead use
     *   {@link AbstractRule#ruleOutputJoinerBuilder(String, List)}
     */
    public static Rule createOutputJoinerRule(String pName, List<String> pRuleNames, char pDelim)
            throws RuleException {
        if (CommonUtils.stringIsBlank(pName)) {
            throw new RuleException("Name cannot be blank when requesting a JoinOutputRule");
        }

        return new JoinOutputRule.Builder(pName, pRuleNames).delimiter(pDelim).build();
    }


    /**
     * Returns a {@link JoinOutputRule.Builder} that can be used to create {@link JoinOutputRule.Builder} objects
     * by invoking {@link JoinOutputRule.Builder#build()}. This
     * object, when invoked, executes the rules passed in {@param pRuleNames}, and concatenates the outputs, separated
     * by a blank space. You can override the separator to use. Simply call {@link JoinOutputRule.Builder#delimiter} before
     * calling {@link JoinOutputRule.Builder#build()}. Read the documentation of {@link JoinOutputRule.Builder} to
     * familiarize yourself with all the optional parameters available.
     *
     * @param pName - A unique name that can be used to refer to this rul object. If name is not unique, then
     *              {@link DuplicateCacheEntryException} gets thrown.
     * @param pRuleNames - A list of the names of the rules which will get executed.
     * @return - A {@link JoinOutputRule.Builder} object which you can then use to create the {@link JoinOutputRule}
     *   rule object by invoking {@link JoinOutputRule.Builder#build()}.
     */
    public static JoinOutputRule.Builder ruleOutputJoinerBuilder(String pName, List<String> pRuleNames) {
        return new JoinOutputRule.Builder(pName, pRuleNames);
    }


    /**
     *
     * @param pName
     * @param pNameValPairs
     * @return
     */
    public static ContextAugmentingRule.Builder contextAugmenterBuilder(
            String pName,
            Map<String, String> pNameValPairs) {
        return new ContextAugmentingRule.Builder(pName, pNameValPairs);
    }


    /**
     *
     * @param pName
     * @param pDbSrcName
     * @param pQry
     * @return
     */
    public static DatabaseOperationRule.Builder databaseOperationBuilder(
            String pName,
            String pDbSrcName,
            String pQry) {
        return new DatabaseOperationRule.Builder(pName, pDbSrcName, pQry);
    }



    /**
     *
     * @return
     */
    public static Rule createGenericRelOpRule() {
        return createRelOpRule(null);
    }


    public static Rule createGenericRegExRule() {
        String cacheKey = generateCacheKey(new String[] {RegExRule.GENERIC_CACHE_KEY});
        Rule rule = ruleCache.get(cacheKey);
        if (null == rule) {
            rule = new RegExRule(null);
            rule = uniqueRule(rule, cacheKey);
        }

        return rule;
    }




    public static Rule createGenericLookupRule() {
        return createLookupRule(LookupRuleExecutor.GENERIC_CACHE_KEY, null);
    }


    public static Rule createJavaScriptFunctionRule(String pName, Object pFuncObj)
            throws RuleException {
        Rule r = new JavaScriptFunctionRule(pFuncObj, pName);
        storeInRuleByNameCache(r);
        return r;
    }


    public static Rule createLookupRule(String pListName, List<String> pOutFlds) {
        return lookupRuleExecutorBuilder(pListName).outputFields(pOutFlds).build();
    }


    public static LookupRuleExecutor.Builder lookupRuleExecutorBuilder(String pListName) {
        return new LookupRuleExecutor.Builder(pListName);
    }




    public static Rule createGenericCheckAgainstCollectionRule() {
        String cacheKey = generateCacheKey(new String[] {CheckAgainstCollectionRule.GENERIC_CACHE_KEY});
        Rule rule = ruleCache.get(cacheKey);
        if (null == rule) {
            rule = new CheckAgainstCollectionRule();
            rule = uniqueRule(rule, cacheKey);
        }

        return rule;
    }

    /**
     *
     * @param pRelationalOp
     * @return
     */
    public static Rule createRelOpRule(String pRelationalOp) {
        return createRelOpRuleWithFixedOperands(pRelationalOp, RelOpRule.IGNORE_INT, RelOpRule.IGNORE_INT);
    }



    /**
     *
     * @param pRelationalOp
     * @param pLeftOperand
     * @param pRightOperand
     * @return
     */
    public static Rule createRelOpRuleWithFixedOperands(
            String pRelationalOp,
            int pLeftOperand,
            int pRightOperand) {
        String cacheKey =
                generateCacheKey(new String[] {
                        pRelationalOp,
                        Integer.toString(pLeftOperand),
                        Integer.toString(pRightOperand)});
        Rule rule = ruleCache.get(cacheKey);

        if (null == rule) {
            if (pLeftOperand != RuleConstants.IGNORE_INT && pRightOperand != RuleConstants.IGNORE_INT) {
                rule = new RelOpRule(pRelationalOp, null, pLeftOperand, pRightOperand);
            } else {
                rule = new RelOpRule(pRelationalOp);
            }
            rule = uniqueRule(rule, cacheKey);
        }

        return rule;
    }

    public static Rule createStringHandlerRule(String pOperator) throws DuplicateCacheEntryException {
        return createStringHandlerRule(pOperator, null);
    }

    public static Rule createStringHandlerRule(String pOperator, String pStringOperator)
            throws DuplicateCacheEntryException {
        String[] cacheKeyAry;
        if (CommonUtils.stringIsBlank(pStringOperator)) {
            cacheKeyAry = new String[]{pOperator};
        } else {
            cacheKeyAry =  new String[]{pOperator, pStringOperator};
        }
        String cacheKey = generateCacheKey(cacheKeyAry);
        Rule rule = ruleCache.get(cacheKey);

        if (null == rule) {
            rule = new StringHandlerRule(pOperator, pStringOperator);
            rule = uniqueRule(rule, cacheKey);
            storeInRuleByNameCache(rule);
        }

        return rule;
    }

    public static Rule createRuleFlow(Object pData, String pRuleFlowName, List<String> pOutFields) throws RuleException {
        if (CommonUtils.stringIsBlank(pRuleFlowName)) {
            throw new RuleException("Name cannot be blank.");
        }

        Rule rule = ruleFlowBuilder(pData, pRuleFlowName).outputFields(pOutFields).build();
        return rule;
    }


    public static RuleFlowImpl.Builder ruleFlowBuilder(Object pData, String pRuleFlowName) {
        return new RuleFlowImpl.Builder(pData, pRuleFlowName);
    }


    public static Rule createCheckAgainstCollectionRule(List<String> pCollection) {
        return createCheckAgainstCollectionRuleForOperationWithFixedValueToCheck(pCollection, null, null);
    }

    public static Rule createCheckAgainstCollectionRuleForOperation(
            List<String> pCollection, String pOp) {
        return createCheckAgainstCollectionRuleForOperationWithFixedValueToCheck(pCollection, pOp, null);
    }

    public static Rule createCheckAgainstCollectionRuleForOperationWithFixedValueToCheck(
            List<String> pCollection,
            String pOp,
            String pValToCheck) {
        String[] sortedCollMembers;
        if (CommonUtils.stringIsBlank(pOp) && CommonUtils.stringIsBlank(pValToCheck)) {
            sortedCollMembers = pCollection.stream().sorted().toArray(String[]::new);
        } else {
            // Either pOp or pValToCheck or both have a non-null value
            List<String> keyList = new ArrayList<>(pCollection);
            if (!CommonUtils.stringIsBlank(pOp)) {
                keyList.add(pOp);
            }
            if (!CommonUtils.stringIsBlank(pValToCheck)) {
                keyList.add(pValToCheck);
            }
            sortedCollMembers = keyList.stream().sorted().toArray(String[]::new);
        }

        /**
         * If an object has already been cached that will do the same exact operation,
         * use it instead of creating a new one.
         */
        String cacheKey = generateCacheKey(sortedCollMembers);
        Rule rule = ruleCache.get(cacheKey);
        if (null == rule) {
            rule = new CheckAgainstCollectionRule(pCollection, null, pOp, pValToCheck);
            rule = uniqueRule(rule, cacheKey);
        }

        return rule;
    }


    /**
     * Invoke this method *only* during initialization
     */
    public static void initializeDataSourceJoinInformationForQueryableRuleConfigComponents()
            throws RuleException {
        logDebug("Starting data source information initialization...");
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            if (!(ruleEnt.getValue() instanceof IQueryableDataRule)) {
                continue;
            }
            ((QueryableDataRule) ruleEnt.getValue()).initializeDataSourceJoinInformation();
        }
        logDebug("Done with data source information initialization.");
        validateQueryableDataRuleComponents();
        /**
         * IMPORTANT: The mapper init call must be done *after* regular joins are
         *  detected. Why? Because only after regular join detection phase is when we will know
         *  all data mappers and the sources the join, and only after that is when we should attempt
         *  do automatically establish a join back to the data mapper from the the two mapper's target
         *  data sources.
         */
        initializeDataMapperJoinInfo();

        postJoinInitializationValidations();
        preAssembleLookupComponentDataSources();
    }


    /**
     * Pre-assembles look up lists, because doing assembly on the fly every single time for every
     * document being processed by Rule API can be quite expensive, and very slow.
     * For example, if a lookup list is made up of thousands of list entries, we wouldn't want to
     * assemble every single time for each request. Assembly is very slow when the target data source yields
     * a lot of rows. One possible explanation is that the {@link Context} API might be doing a lot of
     * CPU intensive operations under the hood, to create a data source like {@code Context} structure. When
     * thousands of entries are involved, then that many times those CPU intensive operations have to be
     * unnecessarily repeated, resulting in the slow response.
     * Also, only those lists are cached which associated entry data source meets row count threshold. For those lists
     * we still cache the entries, because those are where the biggest slowness is observed. But for lists
     * which fully assembled context does get cached, we do not store in cache the entries for those, as that is
     * unnecessary and redundant, and would needlessly consume memory.
     *
     *
     * @throws RuleException
     */
    static void preAssembleLookupComponentDataSources() throws RuleException {
        logDebug("Starting pre-assembly of list lookup components...");
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            Rule curRuleComponent = ruleEnt.getValue();
            if (!dataSourceIsListContainer(curRuleComponent)) {
                /*
                 * Not a list lookup component, skip
                 */
                continue;
            }
            QueryableLookupRule listQdr = (QueryableLookupRule) curRuleComponent;

            IQueryableDataRule entryComp = listQdr.associatedEntryComponent();
            /**
             * To conserve memory, load into memory only the entries for the look up
             * component currently being iterated over, even if we end up doing this
             * multiple times in this loop (I.e. in case more than one component shares the same entry data
             * source). Then once done with the lists of this component, removed said entries
             * are removed from memory further below (except if the list context did not get cached because
             * its entry data source did not meet row count cache threshold, in which case those entries
             * remain in memory). Anyways, loading entries into a map is a fairly quick
             * and cheap operation.
             */
            String entryDsName = entryComp.dataSourceName();
            loadListEntryDataSourcesToMap(entryDsName);

            if (!entryRowCountCacheThresholdMet(listQdr)) {
                logDebug("Component " + listQdr.name() + " + will *not* have its list context's pre-assembled, "
                        + "because associated entry component/data source " + entryComp.name() + "/"
                        + entryComp.dataSourceName() + " row count of " + entryComp.rowCount()
                        + " does not meet threshold for caching.");
                continue;
            }


            Context luListCtx = listQdr.context();
            List<String> luListNames =
                    luListCtx.memberValue(luListCtx.startSearchPath().toString()).asArray().stream().
                            map(e -> e.memberValue(QueryableLookupRule.LIST_NAME_FLD_NAME).stringRepresentation()).
                            collect(Collectors.toList());
            for (String l : luListNames) {
                logDebug("Pre-assembling list " + l + " of component " + listQdr.name());
                listQdr.assembleListContext(l, null);
                logDebug("Done pre-assembling list " + l + " of component " + listQdr.name());
            }
            logDebug("Will remove " + entryDsName + " from entries map, which currently contains these keys: "
                    + listEntryContextLookupMap.entrySet().stream()
                    .map(e -> e.getKey()).collect(Collectors.joining("\n", "\n", "\n")));
            listEntryContextLookupMap.remove(entryDsName);
            logDebug("Removed " + entryDsName + " from entries map, only these keys now remain: "
                    + listEntryContextLookupMap.entrySet().stream()
                    .map(e -> e.getKey()).collect(Collectors.joining("\n", "\n", "\n")));
        }

        logDebug("Done pre-assembling list lookup components. List names with context currently in cache are "
                + listAssembledContextCache.entrySet().stream().map(e -> e.getKey())
                .collect(Collectors.joining("\n", "\n", "\n")));
        logDebug("Entry data sources cached are " + listEntryContextLookupMap.entrySet().stream().map(e -> e.getKey())
                .collect(Collectors.joining("\n", "\n", "\n")));
    }


    /**
     * For every list entry data source, create a {@code Map} where key is the entry ID, and the value
     * is the {@link Context} for the list entry. Then put each {@code Map} into another {@code Map}, where the key
     * is the list entry data source name.
     * This method should be invoked during initialization stage. This is done so that any data source component
     * that wishes to use a list entry data source, can get an entry much quicker, as opposed to for instance having
     * to do assembly of the list entry data source in the fly for each request, which can be quite expensive
     * and very slow. See {@link AbstractRule#preAssembleLookupComponentDataSources()} for more info.
     * @throws TopLevelElementValueIsNotArrayException
     */
    private static void loadListEntryDataSourcesToMap() throws TopLevelElementValueIsNotArrayException {
        loadListEntryDataSourcesToMap(null);
    }

    /**
     * Same as {@link AbstractRule#loadListEntryDataSourcesToMap()}, but this will only load the entries
     * for the specified entry data source.
     *
     * @param pEntryDataSourceName
     * @throws TopLevelElementValueIsNotArrayException
     */
    private static void loadListEntryDataSourcesToMap(String pEntryDataSourceName)
            throws TopLevelElementValueIsNotArrayException {
        logDebug("Loading list entries to map" + (!CommonUtils.stringIsBlank(pEntryDataSourceName)
                ? " for entry data source " + pEntryDataSourceName : "") + "...");
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            Rule curRuleComponent = ruleEnt.getValue();
            if (!dataSourceIsEntryContainer(curRuleComponent)) {
                continue;
            }
            QueryableDataRule qdr = (QueryableDataRule) curRuleComponent;
            if (!CommonUtils.stringIsBlank(pEntryDataSourceName)
                    && !pEntryDataSourceName.equals(qdr.dataSourceName())) {
                continue;
            }

            Context listEntryCtx = qdr.context();
            String dsName = qdr.dataSourceName();
            List<Context> rows = listEntryCtx.memberValue(dsName).asArray();
            Map<String, Context> m = rows.stream().collect(Collectors.toMap(e ->
                            QueryableDataRule.ID_FLD_NAME + "="
                                    + e.memberValue(QueryableDataRule.ID_FLD_NAME).stringRepresentation(),
                    e -> e));
            listEntryContextLookupMap.put(dsName, m);
            logDebug("Loaded " + dsName + " to map.");
        }
        logDebug("Done loading list entries to map" + (!CommonUtils.stringIsBlank(pEntryDataSourceName)
                ? " for entry data source " + pEntryDataSourceName : "") + ".");
    }


    static boolean dataSourceIsListContainer(Rule pRule) throws TopLevelElementValueIsNotArrayException {
        if (!(pRule instanceof IQueryableDataRule)) {
            return false;
        }

        QueryableDataRule qdr = (QueryableDataRule) pRule;
        if (!qdr.fieldNames().contains(QueryableLookupRule.LIST_NAME_FLD_NAME)) {
            return false;
        }

        return true;
    }


    static boolean dataSourceIsEntryContainer(Rule pRule) throws TopLevelElementValueIsNotArrayException {
        if (!(pRule instanceof IQueryableDataRule)) {
            return false;
        }

        QueryableDataRule qdr = (QueryableDataRule) pRule;
        if (!qdr.fieldNames().contains(QueryableLookupRule.LIST_ENTRY_FLD_NAME)) {
            return false;
        }

        return true;
    }


    /**
     * Auto-detect data mappers, and create a join to the data mapper from the data source that the data mapper
     * acts on. Because a data mapper correlates two and only two data sources, this means that for every
     * data mapper, two joins will be auto-created on the data sources that the data mapper correlates
     * @throws RuleException
     */
    private static void initializeDataMapperJoinInfo() throws RuleException {
        logDebug("Auto-detecting joins with other mapper data sources if any...");
        Set<Map.Entry<String, Rule>> ruleByNameEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ent : ruleByNameEnts) {
            if (!(ent.getValue() instanceof QueryableDataRule)) {
                continue;
            }

            QueryableDataRule qdr = (QueryableDataRule) ent.getValue();
            if (!qdr.dataSourceIsMapper() || qdr.getDataSourceJoinInformation().isEmpty()) {
                continue;
            }

            /*
             * Link the data source that wishes to use this mapper data source, by adding a new join object to it that links
             * it to this data mapper
             */
            for (DataSourceJoinInformation dsji : qdr.getDataSourceJoinInformation()) {
                /*
                 * The other data source that wishes to link up with this data mapper gets its "id" field assigned
                 * as join source field, and its target join field gets assigned this data mapper's source join field.
                 * Remember the mapper's target data sources were detected above during plain old regular join
                 * detection.
                 */
                DataSourceJoinInformation newTargetDsjiEntry =
                        new DataSourceJoinInformation(qdr, QueryableDataRule.ID_FLD_NAME, dsji.getSourceJoinFieldName());
                QueryableDataRule targetQdr = (QueryableDataRule) dsji.getTargetDataSource();
                logDebug("Found a join with a data mapper. Data source component '" + targetQdr.name()
                        + "' joins with maper data source '" + qdr.name() + "'. Join info created is "
                        + newTargetDsjiEntry.toString());
                targetQdr.getDataSourceJoinInformation().add(newTargetDsjiEntry);
            }
        }
        logDebug("Done auto-detecting joins with other mapper data sources.");
    }


    private static void postJoinInitializationValidations() throws RuleException {
        logDebug("Starting post join initialization validation of queryable components...");
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            if (!(ruleEnt.getValue() instanceof IQueryableDataRule)) {
                continue;
            }
            QueryableDataRule qdr = (QueryableDataRule) ruleEnt.getValue();
            qdr.performPostJoinInitializationValidations();
        }
        logDebug("Done with post join initialization validation of queryable components.");
    }


    static void validateQueryableDataRuleComponents()
            throws RuleException {
        logDebug("Starting validation of queryable components...");
        Set<Map.Entry<String, Rule>> ruleEnts = getRuleByNameCache().entrySet();
        Map<String, QueryableDataRule> seenTableNames = new HashMap<>();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            if (!(ruleEnt.getValue() instanceof IQueryableDataRule)) {
                continue;
            }
            QueryableDataRule qdr = (QueryableDataRule) ruleEnt.getValue();
            qdr.validateQueryableDataFormat();

            /**
             * Block below enforces unique data source names. The data source name is the top level
             * outer most single element. Remember that data sources, as long as the String.equals() method shows
             * they're exactly the same, can be shared among rule objects, so we
             * might come across the same name twice, because we're iterating over all QueryableDataRule objects.
             * The check below accounts for such a scenario.
             * See {@link QueryableDataRule#QueryableDataRule(Object, String)} for more details.
             */
            String tableName = qdr.dataSourceName();
            if (seenTableNames.containsKey(tableName) && qdr.context() != seenTableNames.get(tableName).context()) {
                throw new DuplicateDataSourceNameException(seenTableNames.get(tableName), qdr);
            }
            seenTableNames.put(tableName, qdr);

        }
        logDebug("Done with validation of queryable components.");
    }


    /**
     * If the pRule passed in was not already in cache, store it and return it, otherwise
     * return the rule object that was already in cache for the given pCacheKey key. In case of the latter
     * the passed in object will get re-claimed by the JVM garbage collector once it goes out
     * scope, namely when the calling method(s) that pass the reference here exits.
     */
    static Rule uniqueRule(Rule pRule,
                           String pCacheKey) {
        Rule prevRule = ruleCache.putIfAbsent(pCacheKey, pRule);
        return null == prevRule ? pRule : prevRule;
    }


    /**
     * Creates and stores in cache a {@link DataSource} connection pool
     * @param pName - The key to use to store in cache
     * @param pConnPoolPropsFilePath - Path to file which contains pool configuration properties.
     * @throws IOException
     * @throws DuplicateCacheEntryException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static void registerDatabaseSource(String pName, String pConnPoolPropsFilePath)
            throws IOException, DuplicateCacheEntryException,
            NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        logInfo("Registering data source '" + pName + "', pool configuration located at "
                + pConnPoolPropsFilePath);
        Map<String, String> props = PropertiesFileUtils.getInstance(pConnPoolPropsFilePath, true)
                .asMap(pConnPoolPropsFilePath);
        PoolProperties pp = new PoolProperties();
        /**
         * Use reflection to initialize pool properties. This has the benefit that if an unexpected config
         * property is encountered, {@link IllegalArgumentException} will be thrown. It's a way to protect
         * a developer against herself.
         */
        for (Map.Entry<String, String> ent : props.entrySet()) {
            String propName = ent.getKey();
            String propVal = ent.getValue();
            PoolProperty p = PoolProperty.fromPropertyName(propName);
            Method m = pp.getClass().getMethod(p.getSetterMethodName(), p.getType());
            if (p.getType() == int.class) {
                m.invoke(pp, Integer.valueOf(propVal));
            } else {
                m.invoke(pp, propVal);
            }
        }

        /*pp.setDriverClassName(props.get("ods_driver"));
        pp.setUrl(props.get("ods_connectionString"));
        pp.setUsername(props.get("ods_username"));
        pp.setPassword(props.get("ods_password"));
        pp.setJdbcInterceptors(props.get("ods_interceptor"));
        pp.setInitialSize(Integer.parseInt(props.get("ods_pool_initial_size")));
        pp.setMaxActive(Integer.parseInt(props.get("ods_pool_max_active")));
        pp.setMaxIdle(Integer.parseInt(props.get("ods_pool_max_idle")));
        pp.setMinIdle(Integer.parseInt(props.get("ods_pool_min_idle")));*/

        DataSource ds = new DataSource(pp);
        DatabaseSource dbSrc = new DatabaseSource.Builder(pName, ds).build();
        storeInCache(dbSrc, pName, databaseSourceByNameCache, true);
        logInfo("Done registering data source '" + pName + "', pool configuration located at "
                + pConnPoolPropsFilePath);
    }

    private enum PoolProperty {
        USERNAME("username", "setUsername", String.class),
        PASSWORD("password", "setPassword", String.class),
        CONNECTION_STRING("connectionString", "setUrl", String.class),
        DRIVER("driver", "setDriverClassName", String.class),
        INTERCEPTOR("interceptor", "setJdbcInterceptors", String.class),
        POOL_INIT_SIZE("poolInitialSize", "setInitialSize", int.class),
        POOL_MAX_ACTIVE("poolMaxActive", "setMaxActive", int.class),
        POOL_MAX_IDLE("poolMaxIdle", "setMaxIdle", int.class),
        POOL_MIN_IDLE("poolMinIdle", "setMinIdle", int.class);

        private final String propertyName;
        private final String setterMethodName;
        private final Class<?> type;

        public String getPropertyName() {
            return propertyName;
        }
        public String getSetterMethodName() {
            return setterMethodName;
        }
        public Class<?> getType() {
            return type;
        }

        PoolProperty(String pName, String pMethName, Class<?> pType) {
            propertyName = pName;
            setterMethodName = pMethName;
            type = pType;
        }

        static PoolProperty fromPropertyName(String pName) {
            for (PoolProperty c: PoolProperty.values()) {
                if (c.getPropertyName().equals(pName)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Unrecognized pool property, got: " + pName);
        }
    }


    /**
     *
     * @param pRule
     * @throws DuplicateCacheEntryException
     */
    static void storeInRuleByNameCache(Rule pRule) throws DuplicateCacheEntryException {
        storeInCache(pRule, null, ruleByNameCache, true);
    }

    /**
     *
     * @param pRule
     * @param pRuleName
     * @throws DuplicateCacheEntryException
     */
    static void storeInRuleByNameCache(Rule pRule, String pRuleName) throws DuplicateCacheEntryException {
        storeInCache(pRule, pRuleName, ruleByNameCache, true);
    }


    /**
     * Stores objects in a Map that allows lookup by name. If trying to use a key which already
     * exists in the Map, then DuplicateCacheEntryException gets thrown.
     * @param pObjToStore - The object to put in the cache
     * @param pObjName - The name of the object to store. This will get used as the key in the cache. If a null
     *                 or blank value is specified, the object's own name is used - obtained by calling
     *                 {@link IdentifiedByName#name()}.
     * @param pMap - The Map to be used as the storage cache
     * @param pHandleRaciness - Set to true if this method will be invoked concurrently. Setting to true ensures
     *                        {@link DuplicateCacheEntryException} gets thrown if interleaved thread execution
     *                        has resulted in attempt to use the same storage key more than once. When false then the method
     *                        will not bother with such checks, meaning the caller is OK that another thread can override
     *                        the object that another thread just stored using the same Map key.
     * @throws DuplicateCacheEntryException - Gets thrown if an object already exists in the Map with the given key. This is
     *                                        a fail-fast approach to alert the author of rules in the JavaScript that
     *                                        they have accidentally used the same key twice.
     */
    private static <T extends IdentifiedByName> void storeInCache(T pObjToStore,
                                                                  String pObjName,
                                                                  Map<String, T> pMap,
                                                                  boolean pHandleRaciness)
            throws DuplicateCacheEntryException {
        String objName = CommonUtils.stringIsBlank(pObjName) ? pObjToStore.name() : pObjName;
        T objFromCache = pMap.get(objName);
        boolean duplicateNameFound = false;
        if (null != objFromCache) {
            duplicateNameFound = true;
        } else {
            if (!pHandleRaciness) {
                pMap.put(objName, pObjToStore);
            } else {
                /**
                 * Because we'll be executing in a concurrent, multi-threaded environment, have to again check (unless
                 * otherwise specified by {@param pHandleRaciness} flag)
                 * if another thread did not insert from the time this thread found NULL rule in "if()". Think about it,
                 * we're in the "else" because the "if()" said rule was not already in cache, then how come down here now it is
                 * in cache!??!!?? Must throw exception to inform the caller. Assumption is we'll be
                 * executed in a multi-threaded environment, hence why this type of check is done.
                 */
                if (null != pMap.putIfAbsent(objName, pObjToStore)) {
                    duplicateNameFound = true;
                }
            }
        }

        if (duplicateNameFound) {
            throw new DuplicateCacheEntryException(objName, objFromCache);
        }
    }


    /**
     *
     * @param pDataStr
     * @param pElemSearchPath
     * @param pElemFilter
     * @param pTargetElems
     * @param pExtraParams
     * @return
     * @throws RuleException
     * @deprecated - Instead use {@link AbstractRule#apply(Context, SearchPath, Filter, TargetElements, Map)},
     *   because it offers less room for error when specifying input arguments.
     */
    @Override
    public RuleExecutionResult apply(String pDataStr,
                                     String pElemSearchPath,
                                     Map<String, String> pElemFilter,
                                     Set<String> pTargetElems,
                                     Map<String, String> pExtraParams) throws RuleException {
        throw new UnsupportedOperationException("Child classes must override this method");
    }


    /**
     *
     * @param pMap
     * @return
     */
    private Map<String, String> stashParameters(Map<String, String> pMap) {
        if (null == pMap || pMap.isEmpty()) {
            return null;
        }
        Map<String, String> retMap = new HashMap<>();
        for (String p : stashParameters) {
            if (pMap.containsKey(p)) {
                retMap.put(p, pMap.get(p));
            }
        }
        return retMap;
    }


    /**
     *
     * @param pSrcMap
     * @param pTargetMap
     * @return
     */
    private Map<String, String> restoreParameters(Map<String, String> pSrcMap, Map<String, String> pTargetMap) {
        if (null == pSrcMap|| pSrcMap.isEmpty()) {
            return null;
        }

        for (String p : stashParameters) {
            if (pSrcMap.containsKey(p)) {
                pTargetMap.put(p, pSrcMap.get(p));
            }
        }
        return pTargetMap;
    }


    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult apply(Context pContext,
                              T pSearchPath,
                              U pFilter,
                              V pTargetElems,
                              Map<String, String> pExtraParams) throws RuleException {
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }

        /**
         * Push extra params configured for stashing, see {@link this#stashParameters}
         */
        Map<String, String> savedParams = stashParameters(pExtraParams);


        if (null != pContext) {
            /**
             * In the extra parameters map we store the original context as-is that was passed in pContext method argument, so
             * that any rule that's interested can have access to it for whatever reason. This is a one time operation during
             * the lifetime of an incoming record.
             */
            if (!pExtraParams.containsKey(RuleConstants.ARG_ORIGINAL_CONTEXT)) {
                pExtraParams.put(RuleConstants.ARG_ORIGINAL_CONTEXT, pContext.stringRepresentation());
            }

            /**
             * This on the other hand contains the current context, I.e. the one used in current execution
             * of this method. It gets overridden every single time
             */
            pExtraParams.put(RuleConstants.CURRENT_CONTEXT, pContext.stringRepresentation());
        }


        try {
            /**
             * For performance tracking purposes, store start time in nano's of this rule. Do this before
             * the body of the rule executes
             */
            pExtraParams.put(RuleConstants.NANO_START_TIME, String.valueOf(System.nanoTime()));
            return applyBody(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams);
        } catch (Throwable pThrown) {
            /**
             * Catch any {@link RuntimeException}, just to be able to provide useful troubleshooting
             * info, then re-throw it as a {@link RuleException}.
             */
            if (pThrown instanceof RuntimeException) {
                /**
                 * Read {@link AbstractRule#convertToRuleException(String, Exception)} to see why
                 * we invoke that method here.
                 */
                String ruleName = "'" + name() + "'";
                if (null != pExtraParams && pExtraParams.containsKey(RuleConstants.ARG_TARGET_RULE_NAME)) {
                    ruleName += " (for help debugging, target rule name was '"
                            + pExtraParams.get(RuleConstants.ARG_TARGET_RULE_NAME) + "')";
                }
                throw convertToRuleException("Runtime issue encountered while executing rule " + ruleName
                        + ", please check detailed exception message(s) for more details.", (RuntimeException) pThrown);
            } else {
                throw pThrown;
            }
        } finally {
            if (null != savedParams && !savedParams.isEmpty()) {
                /**
                 * Now "pop" back the stashed parameters, see {@link this#stashParameters}
                 * TODO: FIX ME!!! By the time we restore the parameters, it's already too late because the
                 * TODO:  rule will have already built the rule execution result object which may/will depend on some
                 * TODO:  of the parameters we restored here, after the fact!!!
                 */
                restoreParameters(savedParams, pExtraParams);
            }
        }
    }


    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        throw new UnsupportedOperationException("Child classes must override this method");
    }


    /**
     * Helper method to populate certain common values into a Map, to be used later to display debug messages
     * and the like.
     * @param pContext - Context used to execute this rule
     * @param pSearchPath - Path used to search element in the {@link Context} to which rule was applied.
     * @param pFilter - The {@link Filter} used to help locate the <code>pTargetData</code>
     * @param pTargetElems - Used to further narrow down the selected element(s) to which the rule gets applied
     * @param pExtraParams - Allows client to pass arbitrary list of name/value pairs to alter behavior of rule.
     * @param pTargetData - The data on which rule was applied
     * @return - Populate {@link Map} with the values that can be printed out.
     */
    Map<String, String> populateCommonResultProperties(Context pContext,
                                                       SearchPath pSearchPath,
                                                       Filter pFilter,
                                                       TargetElements pTargetElems,
                                                       Map<String, String> pExtraParams,
                                                       SearchResult pTargetData) {
        // Use LinkedHashMap to return entries in same order inserted
        Map<String, String> info = new LinkedHashMap<>();
        info.put(RuleConstants.RULE_CLASS, this.getClass().getName());
        if (pExtraParams.containsKey(RuleConstants.ARG_TARGET_RULE_NAME)) {
            String argPrefixRemoved = RuleConstants.ARG_TARGET_RULE_NAME.substring(3);
            try {
                Rule targetRule = lookupRuleByName(pExtraParams.get(RuleConstants.ARG_TARGET_RULE_NAME));
                info.put(argPrefixRemoved, targetRule.name());
            } catch (RuleNameNotFoundException e) {
                info.put(argPrefixRemoved, "Could not find target rule name: " + e.toString());
            }
        }
        info.put(RuleConstants.RULE_ID, String.valueOf(id));
        info.put(RuleConstants.RULE_NAME, name());
        Set<Map.Entry<String, Context>> entries = null;
        if (null != pTargetData) {
            entries = pTargetData.entrySet();
        }
        Context targetVal = null;
        if (null != entries && !entries.isEmpty()) {
            targetVal = entries.iterator().next().getValue();
        }
        info.put(RuleConstants.TARGET_VAL, null == targetVal ? "NONE" : targetVal.stringRepresentation());
        info.put(RuleConstants.SEARCH_PATH, null == pSearchPath ? "NONE" : pSearchPath.toString());

        /*
         * Include selected elements (I.e. pTargetElements) if any.
         */
        if (null != pTargetElems) {
            info.put("Target Element(s)", pTargetElems.parallelStream().collect(Collectors.joining(",")));
        }

        /*
         * Display the specified filter, if any
         */
        if (null != pFilter) {
            info.put("Filter", pFilter.entrySet().parallelStream().map(Map.Entry::toString).collect(Collectors.joining(";")));
        }

        // Store the start time recorded earlier
        info.put(RuleConstants.NANO_START_TIME, pExtraParams.get(RuleConstants.NANO_START_TIME));
        info.put(RuleConstants.CURRENT_CONTEXT, pExtraParams.get(RuleConstants.CURRENT_CONTEXT));

        return info;
    }



    SearchPath convertToSearchPath(String pSearchPath) {
        return SearchPath.valueOf(pSearchPath);
    }

    Filter convertToFilter(Map<String, String> pFilterMap) {
        return Filter.fromMap(pFilterMap);
    }

    TargetElements convertToTargetElements(Set<String> pTargetElems) {
        return TargetElements.fromSet(pTargetElems);
    }



    static String generateCacheKey(String[] pTokens) {
        if (pTokens.length == 1) {
            return pTokens[0];
        }
        StringBuilder key = new StringBuilder();
        for (String t : pTokens) {
            key.append(CACHE_KEY_TOKEN_SEPARATOR);
            key.append(t);
        }

        return key.toString();
    }

    static void logError(Exception pExc) {
        _LOGGER.error(prefixWithThreadName(CommonUtils.extractStackTrace(pExc).toString()));
    }

    static void logWarning(Exception pExc) {
        _LOGGER.warn(prefixWithThreadName(CommonUtils.extractStackTrace(pExc).toString()));
    }

    static void logDebug(String pStr) {
        _LOGGER.debug(prefixWithThreadName(pStr));
    }

    static void logInfo(String pStr){
        _LOGGER.info(prefixWithThreadName(pStr));
    }

    static String prefixWithThreadName(String pMsg) {
        StringBuilder sb = new StringBuilder();
        Thread t = Thread.currentThread();
        if (null == t) {
            return pMsg;
        }
        sb.append("<");
        sb.append(t.getName());
        sb.append("> - ");
        sb.append(pMsg);
        return sb.toString();
    }


    static SearchResult buildFakeSearchResult(String pVal) {
        Map<String, Context> fakeResMap = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        /**
         * ContextFactory.INSTANCE.obtainContext() will fail to build a Context object if it contains spaces,
         * therefore replace spaces with something else
         */
        sb.append("\"");
        sb.append(pVal.replaceAll("\\s", EscapeUtil.ESCAPE_SPACE));
        sb.append("\"");

        fakeResMap.put(RuleConstants.EXPLICIT_RULE_EVAL_TARGET, ContextFactory.INSTANCE.obtainContext(sb.toString()));
        return SearchResult.createSearchResult(fakeResMap);
    }


    private Map<String, Rule> initOutFieldMap(List<String> pOutFlds)
            throws RuleNameNotFoundException {
        if (null == pOutFlds || pOutFlds.isEmpty()) {
            return null;
        } else {
            Map<String, Rule> outFldToRule = new LinkedHashMap<>();
            for (String e : pOutFlds) {
                String[] ruleAndKey = e.split(Pattern.quote("||"));
                outFldToRule.put(ruleAndKey[1], lookupRuleByName(ruleAndKey[0]));
            }
            return outFldToRule;
        }
    }

    static boolean entryRowCountCacheThresholdMet(QueryableLookupRule pQryLuComponent) throws RuleException {
        String cacheThreshStr = AbstractRulesEngine.fetchConfigurationPropertyValue(RuleConstants.CONFIG_PROP_LIST_CTX_ROW_CNT_CACHE_THRESHOLD);
        IQueryableDataRule entryComp = pQryLuComponent.associatedEntryComponent();
        int entryDsRowCnt = entryComp.rowCount();
        if (!CommonUtils.stringIsBlank(cacheThreshStr)) {
            int rowCntThreshold = Integer.valueOf(cacheThreshStr);
            if (entryDsRowCnt < rowCntThreshold) {
                logDebug("Entry data source " + entryComp.dataSourceName() + " in component " + entryComp.name()
                        + " does not meet configured threshold of " + rowCntThreshold + " rows. It has "
                        + entryDsRowCnt + " rows.");
                return false;
            }
        } else {
            cacheThreshStr = "'UNLIMITED'";
        }

        logDebug("Entry data source " + entryComp.dataSourceName() + " in component " + entryComp.name()
                + " meets configured threshold of " + cacheThreshStr + " rows. It has " + entryDsRowCnt + " rows.");
        return true;
    }


    /**
     * Use this utility method to wrap any type of exception into a {@link RuleException}. If there's an underlying
     * cause, unwrap it. Note that this just unwraps one level. The root cause might
     * be wrapped X number of levels down, which we won't bother unwrapping as the number of levels is
     * variable. We unwrap one level so that our wrapping here won't hide a cause which calling code elsewhere
     * might be expecting at that level; this way we make ourselves as unobtrusive as possible. Remember
     * our goal here is just to show additional info which might be useful for troubleshooting and diagnosis,
     * and that's it. We want to avoid side effects introduced if we were to bury the exception cause another level
     * down.
     *
     * @param pMsg
     * @param e
     * @return - The newly built {@link RuleException} that includes the passed in {@param pMsg}
     */
    public static RuleException convertToRuleException(String pMsg, Exception e) {
        Throwable cause = null != e.getCause() ? e.getCause() : e;
        return new RuleException(pMsg, cause);
    }


    /**
     * See {@link QueryableRuleConfigurationData.Builder#addFieldsToContext(Context, ContextAugmentingRule)}
     * for details.
     *
     * @param pCtx
     * @param pRule
     * @param <T>
     * @return
     * @throws RuleException
     */
    static <T extends ContextAugmentingRule> Context addFieldsToContext(Context pCtx, T pRule) throws RuleException {
        Map<String, String> params = new HashMap<>();
        RuleExecutionResult res = pRule.apply(pCtx, null, null, null, params);
        return ContextFactory.obtainContext(res.evaluateResultAsString());
    }


    /**
     * Helper method that executes execute the rule if the {@param pVal} is prefixed with
     * {@link RuleConstants#PREF_RULE}. Else it just returns the passed
     * in {@param pVal} as is.
     * @param pVal
     * @param pInputCtx
     * @param pParams
     * @return
     * @throws RuleException
     */
    String handleRulePrefixValue(String pVal, Context pInputCtx, Map<String, String> pParams)
            throws RuleException {
        if (pVal.indexOf(RuleConstants.PREF_RULE) != 0) {
            return pVal;
        }
        String ruleName = pVal.substring(RuleConstants.PREF_RULE.length());
        Rule rule = lookupRuleByName(ruleName);
        RuleExecutionResult res = rule.apply(pInputCtx, null, null, null, pParams);
        String retVal = res.evaluateResultAsString();
        return RuleConstants.BLANK_TOKEN.equals(retVal) ? "" : retVal;
    }

    /*
     * Getters/Setters
     */
    @Override
    public Map<String, Rule> getOutputFieldMap() {
        return outputFieldMap;
    }

    @Override
    public long getId() {
        return this.id;
    }

    static Map<String, Rule> getRuleByNameCache() {
        return ruleByNameCache;
    }

    static Map<String, Rule> getRuleCache() {
        return ruleCache;
    }
}
