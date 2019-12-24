package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.*;
import net.jcip.annotations.Immutable;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The role of this class is to provide sets of rules to execute for a given input record. The task of executing the rule operations
 * configured here is delegated to a different class. When configured correctly this component will be intelligent enough
 * to determine what configured rule operations apply to a given input record. See
 * {@link QueryableRuleConfigurationData#QueryableRuleConfigurationData(Builder)} for
 * details on how this component can be configured.
 *
 * Note: The selectionCriteriaXXX member variables are poorly named, because they suggest to an unwitting developer that
 *   they're meant to be {@link SelectionCriteria} only. The can also be outputs of rule's and constants, please
 *   bear this in mind.
 *
 * TODO: Currently lack a way to specify global success and failure values, instead have to repeat
 * TODO: in each row the same success_output and failure_output values. Need support in the
 * TODO: {@link Builder}
 * Created by QuijadaJ on 5/18/2017.
 */
class QueryableRuleConfigurationData extends QueryableDataRule {
    private final static String PREF_NO_EVAL = RuleConstants.PREF_NO_EVAL;
    private final static String PREF_RULE = RuleConstants.PREF_RULE;
    private final SearchPath searchPath;
    private final TargetElements operationElementsToFind;
    private final String operatorFieldName;
    private final String rightOperandFieldName;
    private final String leftOperandFieldName;
    private final String leftOperandValueOverride;
    private final ResultEvaluationOption resultEvaluationOption;
    private final String successOutputFieldName;
    private final String failureOutputFieldName;
    private final boolean canExecuteInParallel;
    // TODO: Why did I add this parameter at rule set op level??? Is it for rule set op granularity in future, meaning that
    // TODO:   one rule set op can behave a one way different from others?
    private final boolean useTargetRuleOutput;
    private final Rule additionalContextFields;
    private final String operatorOverride;
    private final boolean useAsTemplate;
    private final boolean ignoreListNotExistsError;
    private final Map<String, String> copyToCurrentContext;

    final static String MULTI_VAL_DELIM = Utilities.MULTI_VAL_DELIM;

    /**
     * The members below are used to find the values in input record that will be used to key into the rule
     * config data hosted by this component.
     * The selection criteria are split into two Maps. The first is for when the filter value is a selection criteria,
     * the second is for when filter value is a rule which should be evaluated to dynamically fill it in. We split it in
     * this way to avoid having a Map<String, Object> which would mean some errors will not get caught until runtime.
     */
    private final Map<String, SelectionCriteria> selectionCriteria;
    private final Map<String, String> selectionCriteriaDefaults;
    private final Map<String, Rule> selectionCriteriaForRuleEntries;
    private final Map<String, String> selectionCriteriaConstants;


    public static class Builder implements com.exsoinn.ie.util.Builder<QueryableRuleConfigurationData> {
        // Required parameters
        private final Object ruleData;
        private final String ruleName;
        private final String resultEvalOption;


        // Optional parameters
        private String leftOperandFieldName = null;
        private String operatorFieldName = null;
        private String rightOperandFieldName = null;
        private Map<String, String> ruleSetFilter = new HashMap<>();
        private String leftOperandValueOverride = null;
        private String successOutputFieldName = null;
        private String failureOutputFieldName = null;
        private List<String> outputFields = null;
        private boolean canExecInParallel = false;
        private boolean useTargetRuleOutput = false;
        private Rule additionalContextFields = null;
        private String operatorOverride = null;
        private boolean useAsTemplate = false;
        private boolean ignoreListNotExistsError = false;
        private Map<String, String> copyToCurrentContext = null;




        /**
         * Constructor that expects all the required parameters.
         *
         * @param pRuleData
         * @param pRuleName
         * @param pResEvalOpt
         */
        Builder(Object pRuleData, String pRuleName, String pResEvalOpt) {
            ruleData = pRuleData;
            ruleName = pRuleName;
            resultEvalOption = pResEvalOpt;
        }


        /**
         * Same as {@link Builder#rightOperandFieldName(String)} but for the left hand side operand. Unless there's a recognized
         * prefix in front of it (I.e. {@link QueryableRuleConfigurationData#PREF_RULE}), the value
         * found for this field in the underlying rule configuration data is expected to be a
         * {@link SelectionCriteria} that enables this class to find data in the current
         * input record to then plug in as the left hand operand in the rule operation. Again
         * the task of executing rule configurations found by this component is delegated
         * elsewhere.
         * If this method is not called, the default field name is simply "left_operand". Whatever the value is, it'd better
         * exist in the rule set config JSON, else a {@link RuleException} gets thrown at run time.
         * @param pName
         * @return - A {@link Builder} object with the left operand field name set
         */
        public Builder leftOperandFieldName(String pName) {
            leftOperandFieldName = pName;
            return this;
        }

        /**
         * This sets the name of the field that holds the rule operation, as in ">", "IN", "==", etc... By default, if this
         * method is not called, the field name is "operator".
         * @param pName
         * @return
         */
        public Builder operatorFieldName(String pName) {
            operatorFieldName = pName;
            return this;
        }

        /**
         * Sets the name of field that holds the right hand side operand in the rule operation. The default field name
         * is "right_operand". If not found int he rule set JSON, {@link RuleException} gets thrown at run time.
         * @param pName
         * @return - A {@link Builder} object with the right operand field name set
         */
        public Builder rightOperandFieldName(String pName) {
            rightOperandFieldName = pName;
            return this;
        }

        /**
         * This is a {@link Map} where each key corresponds to a field name in the rule set rows, and
         * the value is a double-pipe delimited string which can be parsed into a {@link SelectionCriteria} of exactly three entries
         * each corresponding respectively and in this order to: search path, filter and target elements. See
         * {@link SelectionCriteria} for more details on how the string should be formatted.
         * This parameter is used by the app at run time to determine which rule configurations are applicable to the
         * current input record being evaluated.
         * @param pRuleSetFilter
         * @return - A {@link Builder} object with the rule set filter <code>Map</code> set
         */
        public Builder ruleSetFilter(Map<String, String> pRuleSetFilter) {
            ruleSetFilter = (null == pRuleSetFilter) ? null : new HashMap<>(pRuleSetFilter);
            return this;
        }

        /**
         * This setter allows specifying a parameter  to set a fixed, global left field operand value that
         * applies to the <strong>entire</strong> rule set. This is added as a convenience
         * to the developer so that she doesn't have to replicate the same left operand
         * value across every row of entire rule set, especially when we're talking about
         * a long left operand value. Also bear in mind the value passed here has precedence
         * over left operand value configured in the rule set, meaning that if both are configured, the left operand
         * value used will be the one specified here (I.e. this one always wins)
         * @param pVal
         * @return - A {@link Builder} object with the left operand override value set
         */
        public Builder leftOperandValueOverride(String pVal) {
            leftOperandValueOverride = pVal;
            return this;
        }


        /**
         * Same as {@link Builder#leftOperandValueOverride(String)}, but for the operator.
         * @param pVal
         * @return
         */
        public Builder operatorOverride(String pVal) {
            operatorOverride = pVal;
            return this;
        }

        /**
         * Specify which field to get the success output value from. The value passed here can represent <strong>any</strong>
         * field in the rule set operation row. This afford great flexibility in what you want the output value to be
         * when the rule set operation in questions passes. In addition, prefix {@link QueryableRuleConfigurationData#PREF_RULE}
         * is supported in the success output field value configured in the rule set, which means the output can be
         * dynamically calculated by executing the desired rule.
         * @param pName
         * @return - A {@link Builder} object with the success output field name set
         */
        public Builder successOutputFieldName(String pName) {
            successOutputFieldName = pName;
            return this;
        }

        /**
         * Read documentation of {@link Builder#successOutputFieldName(String)}, same applies here, but for the
         * value to give when the rule set operation does not pass.
         * @param pName
         * @return - A {@link Builder} object with the failure output field name set
         */
        public Builder failureOutputFieldName(String pName) {
            failureOutputFieldName = pName;
            return this;
        }


        /**
         * This parameter allows you to specify what additional output fields should appear in the
         * {@link RuleExecutionResult} object generated from execution this rule set, when the
         * {@link RuleExecutionResult#evaluateResultAsContext()} gets invoked. To ahieve that, format each <code>String</code>
         * in the list as follows:
         * rule_name||label
         *
         * The "rule_name" is the name of a rule that currently exists in memory. The "label" will be the property name used to
         * accompany the rule output in the resulting {@link Context} object that {@link RuleExecutionResult#evaluateResultAsContext()}
         * generates.
         *
         * @param pFlds
         * @return
         */
        public Builder outputFields(List<String> pFlds) {
            outputFields = null == pFlds ? null : new ArrayList<>(pFlds);
            return this;
        }

        /**
         * Tells whether the operations contained in this rule set can be executed in parallel whenever
         * possible. By default <strong>no</strong> parallelism will be attempted. This is designed this way
         * to avoid unintended race conditions in rule sets that rely on order of operation execution
         * @param pVal - True or false
         * @return - A {@link Builder} object with the flag set
         */
        public Builder canExecInParallel(boolean pVal) {
            canExecInParallel = pVal;
            return this;
        }


        /**
         * When set to true, then the output of the target rule will be used to display results. See
         * {@link ConfigurableRuleExecutor.ConfigurableRuleExecutorResult#getOutput()} and
         * {@link ConfigurableRuleExecutor.ConfigurableRuleExecutorResult#evaluateResultAsString()} for more info.
         * Note: Perhaps confusingly, the only type of output right now that is unconditionally obtained
         *   from the target rule operation is {@link ExecutorRuleExecutionResult#evaluateResultAsContext()}
         * @param pVal
         * @return - A {@link Builder} object with the flag set
         */
        public Builder useTargetRuleOutput(boolean pVal) {
            useTargetRuleOutput = pVal;
            return this;
        }


        /**
         * Accepts a {@link ContextAugmentingRule} (or child thereof) that will be used by
         * {@link ConfigurableRuleExecutor.ConfigurableRuleExecutorResult#evaluateResultAsContext()}
         * to augment the generated result <code>Context</code> object with any additional additional fields. See
         * {@link ContextAugmentingRule} and {@link ConfigurableRuleExecutor.ConfigurableRuleExecutorResult#evaluateResultAsContext()}
         * for more details.
         * @param pName
         * @return - A {@link Builder} object
         */
        public Builder additionalContextFieldsRuleName(String pName) {
            try {
                additionalContextFields = lookupRuleByName(pName);
            } catch (RuleNameNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }


        /**
         * When set to true, the {@link this#selectionCriteria} takes one a dual role. It is used as a
         * {@link this#ctx} placeholder replacement <code>Map</code>, and then as the filter to use to select
         * rules applicable to the {@link this#ctx} <code>Context</code>.
         * This feature is provided to minimize the amount of configuration that needs to be written, where the
         * {@link this#ctx} can serve as a template to dynamically build a complete rule set.
         * The placeholders in the template are formed by prepending double underscore at beginning and appending
         * at end of each of the keys in the {@link Builder#ruleSetFilter(Map)}
         * <code>Map</code>. The code will take care of replacing those in this template with the evaluated values.
         * @param pFlag
         * @return - A {@link Builder} object
         */
        public Builder useAsTemplate(boolean pFlag) {
            useAsTemplate = pFlag;
            return this;
        }


        /**
         * Set this flag to true when this rule is a template used to dynamically build the rule config
         * (I.e. {@link this#useAsTemplate} is set to <code>true</code>), and the dynamically generated rule
         * refers to a lookup list that does not exist. The behavior by {@link LookupRuleExecutor} is to
         * create empty results Context when this flag is set to true. See {@link LookupRuleExecutor}
         * for more details.
         * @param pFlag
         * @return - A {@link Builder} object
         */
        public Builder ignoreListNotExistsError(boolean pFlag) {
            ignoreListNotExistsError = pFlag;
            return this;
        }


        /**
         * List of fields that client wishes be copied from the output context of an executed rule operation, to the current
         * context which just got evaluated. This property simply captures the fields. It is the responsibility of the target
         * class responsible for handling the rule operation to take care of actually using the fields here to write. A
         * {@link Map} is used to optionally translate the original field name to something else once copied to the target
         * destination. If name is to remain the same, then Map key/value should be set to the same value by client.
         * something to the current context.
         * @param pFlds
         * @return - A {@link Builder} object
         */
        public Builder copyToCurrentContext(Map<String, String> pFlds) {
            copyToCurrentContext = new HashMap<>(pFlds);
            return this;
        }


        @Override
        public QueryableRuleConfigurationData build() {
            try {
                Rule r = new QueryableRuleConfigurationData(this);
                storeInRuleByNameCache(r, ruleName);
                return (QueryableRuleConfigurationData) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    static final String LEFT_OPERAND_FLD_NAME = "left_operand";
    static final String IGNORE_ELEM_NOT_FOUND_ERR_FLD_NAME = "ignore_element_not_found_error";
    static final String OPERATOR_FLD_NAME = "operator";
    static final String RIGHT_OPERAND_FLD_NAME = "right_operand";
    static final String LEFT_OPERAND_DEFAULT_FLD_NAME = "left_operand_default";
    static final String PRIORITY_FLD_NAME = "priority";
    static final String DEFAULT_SUFFIX = "_default";


    /*
     * Caller can configure custom values to return when rule evaluation succeeds or fails. The below configures
     * *fixed* field names expected to be in the data object. When there, then the values in those fields override
     * the defaults of "true" and "false" respectively for when a rule operation succeeds or fails.
     */
    static final String OUT_VAL_FLD_NAME_SUCC = "success_output";
    static final String OUT_VAL_FLD_NAME_FAIL = "failure_output";

    /*
     * Constructors
     */


    /**
     * For a description of the parameters, refer to
     * {@link AbstractRule#createQueryableRuleConfigurationDataRule(Object, String, Map, String, List)}
     * @throws RuleException - Thrown if anything goes wrong
     */
    private QueryableRuleConfigurationData(Builder pBuilder) throws RuleException {
        super(pBuilder.ruleData, pBuilder.ruleName, pBuilder.outputFields);
        /**
         * Super class will assign a default rule name, but we don't want that. Caller must supply a name, therefore
         * check if whatever was pass was NULL or blank, and throw error if that's the case.
         */
        if (CommonUtils.stringIsBlank(pBuilder.ruleName)) {
            throw new IllegalArgumentException("You must supply a unique name for this rule");
        }
        searchPath = context().startSearchPath();
        leftOperandFieldName =
                CommonUtils.stringIsBlank(pBuilder.leftOperandFieldName) ? LEFT_OPERAND_FLD_NAME : pBuilder.leftOperandFieldName;
        operatorFieldName = CommonUtils.stringIsBlank(pBuilder.operatorFieldName) ? OPERATOR_FLD_NAME : pBuilder.operatorFieldName;
        rightOperandFieldName =
                CommonUtils.stringIsBlank(pBuilder.rightOperandFieldName) ? RIGHT_OPERAND_FLD_NAME : pBuilder.rightOperandFieldName;
        operationElementsToFind =
                TargetElements.valueOf(Arrays.stream(new String[] {leftOperandFieldName, operatorFieldName, rightOperandFieldName})
                        .collect(Collectors.joining(",")));

        selectionCriteria = parseSelectionCriteria(pBuilder.ruleSetFilter);
        selectionCriteriaDefaults = parseSelectionCriteriaDefaults(pBuilder.ruleSetFilter);
        selectionCriteriaForRuleEntries = parseSelectionCriteriaForRuleEntries(pBuilder.ruleSetFilter);
        resultEvaluationOption = ResultEvaluationOption.fromResultEvaluationString(pBuilder.resultEvalOption);
        selectionCriteriaConstants = parseSelectionCriteriaConstants(pBuilder.ruleSetFilter);
        leftOperandValueOverride = pBuilder.leftOperandValueOverride;
        /**
         * Determine what the names of the success/output field names are. This is from where the respective
         * value will be fetched depending on rule operation pass/fail
         */
        successOutputFieldName = CommonUtils.stringIsBlank(pBuilder.successOutputFieldName)
                ? OUT_VAL_FLD_NAME_SUCC : pBuilder.successOutputFieldName;
        failureOutputFieldName = CommonUtils.stringIsBlank(pBuilder.failureOutputFieldName)
                ? OUT_VAL_FLD_NAME_FAIL : pBuilder.failureOutputFieldName;
        canExecuteInParallel = pBuilder.canExecInParallel;
        useTargetRuleOutput = pBuilder.useTargetRuleOutput;
        additionalContextFields = pBuilder.additionalContextFields;
        operatorOverride = pBuilder.operatorOverride;
        useAsTemplate = pBuilder.useAsTemplate;
        ignoreListNotExistsError = pBuilder.ignoreListNotExistsError;
        copyToCurrentContext = pBuilder.copyToCurrentContext;
    }

    /**
     * @deprecated - The use of this enum is deprecated. Use the Builder pattern way to configure the
     *   various rule set parameters. See {@link AbstractRule#ruleSetBuilder(Object, String, String)} and
     *   {@link Builder} for more info. This enum will no longer be maintained. Going forward any new
     *   configuration options added will <strong>not</strong> be added here, but be added setters of optional
     *   parameters in the {@link Builder} class.
     */
    enum ConfigurationOption {
        LEFT_OPERAND_FIELD_NAME("leftOperandFieldName"),
        OPERATOR_FIELD_NAME("operatorFieldName"),
        RIGHT_OPERAND_FIELD_NAME("rightOperandFieldName"),
        LEFT_OPERAND_VALUE_OVERRIDE("leftOperandValueOverride"),
        RESULT_EVALUATION_OPTION("resultEvaluationOption"),
        SUCCESS_OUTPUT_FIELD_NAME("successOutputFieldName"),
        FAILURE_OUTPUT_FIELD_NAME("failureOutputFieldName"),
        CAN_EXECUTE_IN_PARALLEL("canExecuteInParallel"),
        USE_TARGET_RULE_OUTPUT("useTargetRuleOutput");

        private String configurationOption;

        String configurationOption() {
            return configurationOption;
        }

        ConfigurationOption(String pConfigOpt) {
            configurationOption = pConfigOpt;
        }

        static ConfigurationOption fromString(String pConfigOpt) throws IllegalArgumentException {
            for (ConfigurationOption c: ConfigurationOption.values()) {
                if (c.configurationOption().equals(pConfigOpt)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("Invalid/unsupported configuration option specified, got: " + pConfigOpt);
        }
    }


    /**
     * Delegate to appropriate {@link RuleExecutor}
     * @param pContext
     * @param pSearchPath
     * @param pFilter
     * @param pTargetElems
     * @param pExtraParams
     * @param <T>
     * @param <U>
     * @param <V>
     * @return
     * @throws RuleException
     */
    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        return AbstractRule.executeConfigurableRule(name(), pContext, pExtraParams);
    }


    /**
     * Prepares a list of rule configurations for the given input record.
     * @param pInputCtx
     * @return - A {@link List} of {@link QueryableRuleConfigurationDataResult}'s with all pertinent data necessary for an
     *   external entity to execute each rule operation and evaluate the results
     */
    List<QueryableRuleConfigurationDataResult> obtainRuleConfig(Context pInputCtx, Map<String, String> pExtraParams)
            throws RuleException, InterruptedException {
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }
        pExtraParams.put(RuleConstants.NANO_START_TIME, String.valueOf(System.nanoTime()));

        /*
         * TODO: We could have called super.apply(), but then we would have had to put the arguments to search context() in
         * TODO: a Map (code commented out above). Opted for below because it's cleaner and more concise.
         * TODO: To keep things consistent, do as stated above, and get the search results from
         * TODO:  QueryableDataRuleExecutionResult.getSearchResults
         * TODO:  Besides, parent class apply() searches context(), and instead we want to search a dynamically
         * TODO: assembled context which is a combination of the data sources it joins to (if any). Because of this we can't call parent
         * TODO: apply() and must instead write our own logic.
         */

        /**
         * Build the filter to find the rules applicable to this input record, then assemble the Context object
         * against which we will search for the rule configs. Assembly means joining on the fly all the necessary data
         * sources to form a Context. Finally, we want to return a list of QueryableRuleConfigurationDataResult's,
         * which is not the return type of parent's apply() method.
         * When {@link this#useAsTemplate} is <code>true</code>, then the {@link this#selectionCriteria} is also used
         * first to replace placeholders that match the keys with the corresponding values. See
         * {@link this#configFromTemplate(Filter)} for details.
         */
        Filter f = buildFilterToQueryRuleConfigData(pInputCtx);
        Context ruleConfigCtx;
        if (useAsTemplate) {
            ruleConfigCtx = assembleRuleConfigurationData(configFromTemplate(f), null, null);
        } else {
            ruleConfigCtx = context();
        }

        // Now use filter again, this time to perform the assembly to arrive at final rule set
        ruleConfigCtx = assembleRuleConfigurationData(ruleConfigCtx, f, null);


        /**
         * If search failed to return results, try searching for a DEFAULT configuration. This is done
         * by changing all filter values to {@link this#DEFAULT_FIELD_VAL}
         */
        if (null == ruleConfigCtx) {
            Map<String, String> defaultFilter =
                    f.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> DEFAULT_FIELD_VAL));
            ruleConfigCtx = assembleRuleConfigurationData(Filter.fromMap(defaultFilter), null);
        }


        /*
         * If still NULL, give up and return NULL to caller.
         */
        if (null == ruleConfigCtx) {
            throw new NoMatchingRuleSetFound(pInputCtx, this);
        }

        /*
         * Add output overriding fields if these exist in the context element
         */
        Set<String> newTargetElems = new HashSet<>(operationElementsToFind);

        /**
         * Get the names of the fields found in the underlying rule config. These will be the fields
         * included in the rule operation object.
         */
        List<String> dataSrcFldNames =
                ruleConfigCtx.memberValue(ruleConfigCtx.startSearchPath().toString()).asArray().get(0).topLevelElementNames();
        fieldsToInclude(dataSrcFldNames, successOutputFieldName, newTargetElems, true, ruleConfigCtx);
        fieldsToInclude(dataSrcFldNames, failureOutputFieldName, newTargetElems, true, ruleConfigCtx);
        fieldsToInclude(dataSrcFldNames, LEFT_OPERAND_DEFAULT_FLD_NAME, newTargetElems, false, ruleConfigCtx);
        fieldsToInclude(dataSrcFldNames, IGNORE_ELEM_NOT_FOUND_ERR_FLD_NAME, newTargetElems, false, ruleConfigCtx);
        fieldsToInclude(dataSrcFldNames, PRIORITY_FLD_NAME, newTargetElems, false, ruleConfigCtx);



        // Get the rule configs. Filter is NULL because filter was applied above during Context assembly step
        SearchResult res = ruleConfigCtx.findElement(searchPath, null, TargetElements.fromSet(newTargetElems), null);
        Map<String, String> info = populateCommonResultProperties(
                ruleConfigCtx, searchPath, f, operationElementsToFind, pExtraParams, res);


        /*
         * Finally add to a List all the rule config rows found
         */
        List<QueryableRuleConfigurationDataResult> ret = new ArrayList<>();
        if (!res.isEmpty()) {
            Map.Entry<String, Context> resEnt = res.entrySet().iterator().next();
            /*
             * Multiple rows found
             */
            if (resEnt.getValue().isArray()) {
                Iterator<Context> itCtx = resEnt.getValue().asArray().iterator();
                while(itCtx.hasNext()) {
                    Context c = itCtx.next();
                    QueryableRuleConfigurationDataResult q = prepareQueryReturnObject(c, pInputCtx, info, res, pExtraParams);
                    ret.add(q);
                }
            } else {
                /*
                 * A single row found. The Context.findElement() does not return an array when only a single row
                 * is found. This happens when target elements was specified. This is just the way that API behaves.
                 */
                Context c = resEnt.getValue();
                QueryableRuleConfigurationDataResult q =
                        prepareQueryReturnObject(c, pInputCtx, info, res, pExtraParams);
                ret.add(q);
            }
        }

        addQueryReturnObjectsForMultiValuedLeftOperands(ret, pInputCtx);
        if (resultEvaluationOption != ResultEvaluationOption.BATCH) {
            optimizeLookupOperations(ret, pInputCtx);
        }

        /**
         * Sort the rule ops by the priority value provided by the client.
         */
        sortByPriority(ret);
        return ret;
    }


    private void fieldsToInclude(List<String> pListToCheck,
                                 String pFldName,
                                 Set<String> pFldSet,
                                 boolean pThrowErrIfMissing,
                                 Context pSrcCtx) throws MissingOutputFieldException {
        if (pListToCheck.contains(pFldName)) {
            pFldSet.add(pFldName);
        } else if (pThrowErrIfMissing) {
            throw new MissingOutputFieldException(this, pSrcCtx, pFldName);
        }
    }


    private void sortByPriority(List<QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult> pRuleOps) {
        pRuleOps.sort((pRuleOp1, pRuleOp2) -> {
            if (pRuleOp1.priority < pRuleOp2.priority) {
                return -1;
            } else if (pRuleOp1.priority == pRuleOp2.priority) {
                return 0;
            } else {
                return 1;
            }
        });
    }


    /**
     * From the configured {@link QueryableRuleConfigurationData#context()} dynamically build the rule
     * configuration <code>Context</code>. Placeholders will be replaced with the evaluated values
     * of {@link Builder#ruleSetFilter}. Placeholders are expected to be the keys
     * contained in {@link Builder#ruleSetFilter}, <strong>both</strong> appended and prepended with "__". For example, if
     * the map contains a key "some_key", then the placeholder is expected to be like "__some_key__". Only so
     * will the code be able to dynamically replace this value.
     * @param pFilter
     * @return
     */
    Context configFromTemplate(Filter pFilter) {
        if (null == pFilter || pFilter.isEmpty()) {
            return context();
        }

        String tmplAsStr = context().stringRepresentation();
        for (Map.Entry<String, String> e : pFilter.entrySet()) {
            StringBuilder placeHolder = new StringBuilder();
            placeHolder.append("__");
            placeHolder.append(e.getKey());
            placeHolder.append("__");
            tmplAsStr = tmplAsStr.replaceAll(placeHolder.toString(), e.getValue());
        }

        return ContextFactory.obtainContext(tmplAsStr);
    }


    /**
     * Turn into a comma-delimited string left operand values (I.e. lookup inputs) going the same lookup list AND operator. Any
     * duplicates are also removed in the process. The need for this arose from the fact
     * that some left operand selection criteria can yield multiple values in the search results. Some of those values
     * can/will be exactly the same depending on the nature of the underlying data, and/or can go to the same lookup
     * list and lookup operation.
     * This method packages things so tha the lookup list can be traversed only once per unique operation in the rule set.
     *
     * @param pQryRetObjs
     * @param pInputCtx
     * @throws RuleException
     */
    private void optimizeLookupOperations(List<QueryableRuleConfigurationDataResult> pQryRetObjs, Context pInputCtx)
            throws RuleException {
        List<QueryableRuleConfigurationDataResult> newQryRetObjs = new ArrayList<>();
        final String listOperatorDelim = "__UNDERSCORE__";

        /**
         * This inner class is used to store a set of unique left op (aka input) values, and a delimiter-separated
         * string of the various left op sources that produced each input in the set.
         */
        class RuleConfigEntryInfo {
            final QueryableRuleConfigurationDataResult opInfo;
            final Set<String> inputs = new LinkedHashSet<>();
            final Map<String, String> inputToLeftOpSrc = new LinkedHashMap<>();

            RuleConfigEntryInfo(RuleConfigEntryInfo pRcei, QueryableRuleConfigurationDataResult pOpObj) {
                /**
                 * To keep track, copy passed in RuleConfigEntryInfo's inputs to this one, then
                 * further below add the new input.
                 * Do the same for the left input sources (further below)
                 */
                if (null != pRcei) {
                    inputs.addAll(pRcei.inputs);
                    inputToLeftOpSrc.putAll(pRcei.inputToLeftOpSrc);
                }

                String newInput = pOpObj.getLeftOperand();
                inputs.add(newInput);

                StringBuilder sb = new StringBuilder();
                if (inputToLeftOpSrc.containsKey(newInput)) {
                    sb = new StringBuilder(inputToLeftOpSrc.get(newInput));
                    sb.append(RuleConstants.INPUT_ELEMS_DELIM);
                }
                sb.append(pOpObj.getLeftOperandSource());
                inputToLeftOpSrc.put(newInput, sb.toString());

                opInfo = pOpObj;
            }

        }

        final Map<String, RuleConfigEntryInfo> luListNameToOpObj = new HashMap<>();
        /*
         * Iterate over lookup operation lists, squashing duplicate left operand values (I.e. the lookup inputs)
         * values
         */
        for (QueryableRuleConfigurationDataResult qrcdr : pQryRetObjs) {
            /**
             * Skip non-lookup rule operations
             */
            if (ConfigurableRuleExecutor.RuleType.LookupRuleType
                    != ConfigurableRuleExecutor.RuleType.fromSymbol(qrcdr.getOperator())) {
                /**
                 * Leave non-lookup operations untouched. We're only interested in
                 * optimizing lookup ops
                 */
                newQryRetObjs.add(qrcdr);
                continue;
            }

            /**
             * IMPORTANT: The key to use must be a combination of list name and operator. In the same rule set the same list
             *   can appear with different operators being used, for example one occurrence might be doing LU||LIST_DEFINED, and
             *   another may be doing LU||EXACT_MATCH.
             */
            RuleConfigEntryInfo opObj = null;
            final StringBuilder sbListAndOperator = new StringBuilder();
            sbListAndOperator.append(qrcdr.getRightOperand());
            sbListAndOperator.append(listOperatorDelim);
            sbListAndOperator.append(qrcdr.getOperator());
            final String listAndOperator = sbListAndOperator.toString();


            if (luListNameToOpObj.containsKey(listAndOperator)) {
                opObj = luListNameToOpObj.get(listAndOperator);
            }

            opObj = new RuleConfigEntryInfo(opObj, qrcdr);
            luListNameToOpObj.put(listAndOperator, opObj);

        }

        if (luListNameToOpObj.isEmpty()) {
            /*
             * No lookup ops found, therefore nothing to do
             */
            return;
        }

        /**
         * Ok, we've de-dup'ed all duplicate inputs going to the same lookup list AND operator. Now
         * update que QueryableRuleConfigurationDataResult list.
         * In addition, for every list, make the inputs a comma-separated string. The {@link Context} API
         * which is what handles the lookups, is able to accept comma separated strings as the input, and searches
         * the target {@code Context} accordingly. This is an optimization big time, because it means we have
         * to traverse the search {@code Context} only once per lookup list. In one single iteration the list of inputs
         * are checked against each term encountered during the iteration of the list.
         * The input elements and values are stored as a delimited string, as follow:
         *   - For every unique value found, create a set of input elements that contained that value. The sets are delimited
         *     using {@link RuleConstants#INPUT_SET_DELIM}.
         *   - Separate the elements within each set using delimiter {@link RuleConstants#INPUT_ELEMS_DELIM}
         * The values stored in {@link RuleConfigEntryInfo#inputs} have the same ordering imposed as the sets of input elements
         * in {@link RuleConfigEntryInfo#inputToLeftOpSrc}. This means that the same index of an input element set
         * can be used to obtain the corresponding value from {@link RuleConfigEntryInfo#inputs} set list. This is important
         * because calling code can rely on this imposed ordering when decomposing the input elements and their
         * corresponding values.
         */
        for (Map.Entry<String, RuleConfigEntryInfo> luListNameToOpObjEnt : luListNameToOpObj.entrySet()) {
            RuleConfigEntryInfo rcei = luListNameToOpObjEnt.getValue();
            QueryableRuleConfigurationDataResult q = new QueryableRuleConfigurationDataResult.Builder(
                    rcei.inputs.stream().collect(Collectors.joining(RuleConstants.MULTI_VAL_STR_DELIM)),
                    rcei.opInfo.operator, rcei.opInfo.rightOperand)
                    .leftOperandSource(rcei.inputToLeftOpSrc.entrySet()
                            .stream().map(e -> e.getValue()).collect(Collectors.joining(RuleConstants.INPUT_SET_DELIM)))
                    .result(rcei.opInfo.evaluateResult())
                    .params(rcei.opInfo.getParams())
                    .searchResult(rcei.opInfo.getSearchResult())
                    .outputValueSuccess(rcei.opInfo.outputValueSuccess)
                    .outputValueFailure(rcei.opInfo.outputValueFailure)
                    .leftOperandValueOverride(rcei.opInfo.leftOperandValueOverride)
                    .useTargetRuleOutput(rcei.opInfo.useTargetRuleOutput)
                    .inputContext(pInputCtx)
                    .outputFieldMap(getOutputFieldMap())
                    .priority(rcei.opInfo.priority).build();
            newQryRetObjs.add(q);
        }

        pQryRetObjs.clear();
        pQryRetObjs.addAll(newQryRetObjs);
    }


    /**
     * If the left operand is a double-pipe delimited value, then it means the {@code Context} search yielded
     * more than one result. If that's the case, add additional {@code QueryableRuleConfigurationDataResult}'s to
     * the list for each of the left operand values, and for the rest of the {@code QueryableRuleConfigurationDataResult}
     * parameters, simply copy them over from the original {@code QueryableRuleConfigurationDataResult}.
     *
     * @param pQryRetObjs
     */
    private void addQueryReturnObjectsForMultiValuedLeftOperands(
            List<QueryableRuleConfigurationDataResult> pQryRetObjs,
            Context pInputCtx)
            throws RuleException {
        List<QueryableRuleConfigurationDataResult> newQryRetObjs = new ArrayList<>();
        for (QueryableRuleConfigurationDataResult qrcdr : pQryRetObjs) {
            if (qrcdr.getLeftOperand().indexOf(MULTI_VAL_DELIM) >= 0) {
                String[] tokens = qrcdr.getLeftOperand().split(Pattern.quote(MULTI_VAL_DELIM));
                for (String t : tokens) {
                    /*
                     * Create a copy of this QueryableRuleConfigurationDataResult object, passing the left operand
                     * value to use.
                     */
                    QueryableRuleConfigurationDataResult newObj = new QueryableRuleConfigurationDataResult.Builder(
                            t, qrcdr.operator, qrcdr.rightOperand)
                            .leftOperandSource(qrcdr.getLeftOperandSource())
                            .result(qrcdr.evaluateResult())
                            .params(qrcdr.getParams())
                            .searchResult(qrcdr.getSearchResult())
                            .outputValueSuccess(qrcdr.outputValueSuccess)
                            .outputValueFailure(qrcdr.outputValueFailure)
                            .leftOperandValueOverride(qrcdr.leftOperandValueOverride)
                            .useTargetRuleOutput(qrcdr.useTargetRuleOutput)
                            .inputContext(pInputCtx)
                            .outputFieldMap(getOutputFieldMap())
                            .priority(qrcdr.priority).build();
                    newQryRetObjs.add(newObj);
                }
            } else {
                newQryRetObjs.add(qrcdr);
            }
        }

        if (newQryRetObjs.size() > pQryRetObjs.size()) {
            pQryRetObjs.clear();
            pQryRetObjs.addAll(newQryRetObjs);
        }
    }


    /**
     * Builds an object that includes all the data necessary to perform the rule operation: left and right operands,
     * the operation to perform (relational operation, list presence check, lookup list, etc...), values to output in case of
     * success or failure.
     * @param pRuleConfigCtx
     * @param pInputCtx
     * @param pInfo
     * @param pSearchRes
     * @return
     * @throws RuleException
     */
    private QueryableRuleConfigurationDataResult prepareQueryReturnObject(
            Context pRuleConfigCtx,
            Context pInputCtx,
            Map<String, String> pInfo,
            SearchResult pSearchRes,
            Map<String, String> pExtraParams) throws RuleException {
        validateRequiredFields(pRuleConfigCtx);
        /**
         * This block of logic deals with finding the value of the left operand. Unless prefixed with
         * {@link QueryableRuleConfigurationData#PREF_NO_EVAL}, or
         * {@link QueryableRuleConfigurationData#PREF_RULE} the left hand operand is assumed to be a String that can be
         * parsed into a SelectionCriteria object which will be used to search the input data.
         */
        final String leftHandOperand = !CommonUtils.stringIsBlank(leftOperandValueOverride)
                ? leftOperandValueOverride
                : pRuleConfigCtx.memberValue(leftOperandFieldName).stringRepresentation();

        SelectionCriteria leftOperandSelCrit = null;
        boolean ignoreElemNotFoundError = pRuleConfigCtx.containsElement(IGNORE_ELEM_NOT_FOUND_ERR_FLD_NAME)
                && Boolean.valueOf(pRuleConfigCtx.memberValue(IGNORE_ELEM_NOT_FOUND_ERR_FLD_NAME).stringRepresentation());
        if (null == pExtraParams) {
            pExtraParams = new HashMap<>();
        }
        /**
         * Below tells Context element path finder API to ignore it when filtering a list of
         * nested complex objects and the filtering key is not found in one or more of them, in which case
         * those objects are simply excluded from search results. If below flag were not passed to Context API, then default
         * behavior of Context API is to throw {@link IncompatibleSearchPathException} exception
         */
        if (ignoreElemNotFoundError) {
            pExtraParams.put(Context.IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR, "1");
            pExtraParams.put(Context.IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR, "1");
        }
        final String leftOperandVal;
        if (leftHandOperand.indexOf(PREF_NO_EVAL) == 0) {
            // The left operand is just a constant
            leftOperandVal = leftHandOperand.substring(PREF_NO_EVAL.length());
        } else if (leftHandOperand.indexOf(PREF_RULE) == 0) {
            // The left operand value will come from the output of another rule. If that rule gave blank
            // output, see if a default left operand value was configured in the rule set, and get from there instead.
            String ruleRetVal = handleRulePrefixValue(leftHandOperand, pInputCtx, pExtraParams);
            if (CommonUtils.stringIsBlank(ruleRetVal)) {
                leftOperandVal = fetchLefOperandDefaultValue(pInputCtx, pRuleConfigCtx, leftHandOperand);
            } else {
                leftOperandVal = ruleRetVal;
            }
        } else {
            // The left operand value comes from the incoming data document (E.g. input record), by executing
            // the SelectionCriteria configured in the left operand
            leftOperandSelCrit = SelectionCriteria.valueOf(leftHandOperand);

            // This is for case when the Filter portion of search criteria itself relies on rule
            // output to populate its filter key values.
            leftOperandSelCrit = populateRuleOutputInFilter(leftOperandSelCrit, pInputCtx, pExtraParams);
            SearchResult leftOperandSr;
            try {
                leftOperandSr = pInputCtx.findElement(leftOperandSelCrit, pExtraParams);
            } catch (IllegalArgumentException e) {
                throw e;
            }

            /*
             * The below NULL check might not be necessary, but leaving there just in case. Better be defensive
             * than having to spend unnecessary time tracking down a bug where Context.findElement() is returning NULL.
             */
            if (null == leftOperandSr || leftOperandSr.size() > 1) {
                throw new RuleException("Did not find exactly one search result for left-hand operand SelectionCriteria: "
                        + leftOperandSelCrit.toString()
                        + ". Results found is/are "
                        + (null == leftOperandSr ? "NULL RESULTS" : (leftOperandSr.isEmpty() ? "EMPTY RESULTS"
                        : leftOperandSr.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";")))));
            }


            Context srVal = !leftOperandSr.isEmpty() ? leftOperandSr.entrySet().iterator().next().getValue() : null;
            if (null == srVal) {
                /*
                 * Got empty search results, set left operand to empty string, or see if the rule configuration
                 * specified a default value.
                 */
                leftOperandVal = fetchLefOperandDefaultValue(pInputCtx, pRuleConfigCtx, leftOperandSelCrit.toString());
            } else if (srVal.isArray()) {
                /**
                 * Handle scenario where the left hand operand returned more than one row. This is handled by creating
                 * a {@link QueryableRuleConfigurationData#MULTI_VAL_DELIM} separated string of the results. The left
                 * hand operand results, whether single of multiple, are expected to be in a specific format, and this is
                 * enforced by the utility method below. See {@link Utilities#toDelimitedString(List)} for details.
                 */
                try {
                    leftOperandVal = Utilities.toDelimitedString(srVal.asArray());
                } catch (Exception e) {
                    throw convertToRuleException("Problem converting results to delimited string. See all exception details "
                            + "for more details.\nFull search results are: " + srVal.stringRepresentation()
                            + "\nThe left operand selection criteria was " + leftOperandSelCrit.toString(), e);
                }
            } else if (srVal.isPrimitive()){
                leftOperandVal = srVal.stringRepresentation();
            } else {
                throw new RuleException("While trying to populate left operand value, found a result which is neither"
                        + " an array nor a primitive. Search results are: " + srVal.stringRepresentation());
            }
        }


        /**
         * Get success/failure override values, otherwise by default the display values are true/false
         * respectively. See {@link TextOutputRuleExecutionResult#evaluateResultAsString()} for details
         * on logic that displays true/false by default.
         */
        String outValSucc = null;
        if (pRuleConfigCtx.containsElement(successOutputFieldName)) {
            outValSucc = handleRulePrefixValue(pRuleConfigCtx.memberValue(successOutputFieldName).stringRepresentation(),
                    pInputCtx, null);
        }

        String outValFail = null;
        if (pRuleConfigCtx.containsElement(failureOutputFieldName)) {
            outValFail = handleRulePrefixValue(pRuleConfigCtx.memberValue(failureOutputFieldName).stringRepresentation(),
                    pInputCtx, null);
        }

        final String operator;
        if (!CommonUtils.stringIsBlank(operatorOverride)) {
            operator = operatorOverride;
        } else {
            operator = pRuleConfigCtx.memberValue(operatorFieldName).stringRepresentation();
        }

        int priority = pRuleConfigCtx.containsElement(PRIORITY_FLD_NAME)
                ? Integer.valueOf(pRuleConfigCtx.memberValue(PRIORITY_FLD_NAME).stringRepresentation()) : 0;

        return new QueryableRuleConfigurationDataResult.Builder(
                leftOperandVal, operator,
                pRuleConfigCtx.memberValue(rightOperandFieldName).stringRepresentation())
                .leftOperandSource(leftOperandSelCrit == null ? leftHandOperand : leftOperandSelCrit.specialCharactersConverted())
                .result(!pSearchRes.isEmpty())
                .params(pInfo)
                .searchResult(pSearchRes)
                .outputValueSuccess(outValSucc)
                .outputValueFailure(outValFail)
                .leftOperandValueOverride(leftOperandValueOverride)
                .useTargetRuleOutput(useTargetRuleOutput)
                .inputContext(pInputCtx)
                .outputFieldMap(getOutputFieldMap())
                .priority(priority).build();
    }


    private String fetchLefOperandDefaultValue(Context pInputCtx, Context pRuleConfigCtx, String pLeftOpSrc)
            throws RuleException {
        if (!pRuleConfigCtx.containsElement(LEFT_OPERAND_DEFAULT_FLD_NAME)) {
            throw new RuleException("Must configure '" + LEFT_OPERAND_DEFAULT_FLD_NAME + "' in rule set "
                    + "if the left operand criteria can yield no results. Left operand override (if any configured) is "
                    + leftOperandValueOverride + ". Left operand source configured in rule set is '"
                    + pLeftOpSrc + "', and it yielded no results. Rule set name is '" + this.name()
                    + "'\nThe rule set operation configuration is:\n" + pRuleConfigCtx.stringRepresentation()
                    + "\nThe input data was:\n" + pInputCtx.stringRepresentation());
        }

        return pRuleConfigCtx.memberValue(LEFT_OPERAND_DEFAULT_FLD_NAME).stringRepresentation();
    }



    private void validateRequiredFields(Context pRuleConfigCtx) throws RuleException {
        String[] flds = {leftOperandFieldName, operatorFieldName, rightOperandFieldName};
        for (String f : flds) {
            if (!pRuleConfigCtx.containsElement(f)) {
                throw new RuleException("Did not find required field '" + f + "' in rule set '" + this.name()
                        + "'. The rule set row was " + pRuleConfigCtx.stringRepresentation() + ". Ensure rule set configuration"
                        + " is correct and try again.");
            }
        }
    }

    /**
     * If the selection criteria passed in contains any entries which are prefixed
     * with {@link QueryableRuleConfigurationData#PREF_RULE}, invoke those rules using the passed
     * in <code>Context</code> object, and use the output of the rule as the value of the selection criteria
     * field.
     *
     * @param pSelectCrit
     * @param pInputCtx
     * @return
     * @throws RuleException
     */
    private SelectionCriteria populateRuleOutputInFilter(
            SelectionCriteria pSelectCrit,
            Context pInputCtx,
            Map<String, String> pParams)
            throws RuleException {
        Filter f = pSelectCrit.getFilter();
        if (null == f) {
            return pSelectCrit;
        }
        Set<Map.Entry<String, String>> filterEnts = f.entrySet();
        Map<String, String> newFilter = new HashMap<>();
        for (Map.Entry<String, String> e : filterEnts) {
            String filterKeyVal = e.getValue();
            newFilter.put(e.getKey(), handleRulePrefixValue(filterKeyVal, pInputCtx, pParams));
        }
        return SelectionCriteria.fromObjects(pSelectCrit.getSearchPath(), Filter.fromMap(newFilter),
                pSelectCrit.getTargetElements());
    }



    ResultEvaluationOption getResultEvaluationOption() {
        return resultEvaluationOption;
    }

    public boolean isCanExecuteInParallel() {
        return canExecuteInParallel;
    }


    public boolean isIgnoreListNotExistsError() {
        return ignoreListNotExistsError;
    }


    public Rule getAdditionalContextFields() {
        return additionalContextFields;
    }


    public Map<String, String> getCopyToCurrentContext() {
        return copyToCurrentContext;
    }

    /**
     * Returns a {@link Filter} built to search the rule configs hosted by this component that are applicable to
     * this input record. The filter is built with values from underlying input data record, based on the configured
     * {@link SelectionCriteria} <code>selectionCriteria</code> member of this component, which basically tells
     * the element values to get from input record which are then used as the filter to search the rule
     * set {@code Context}.
     *
     * @param pCtx
     * @return
     */
    Filter buildFilterToQueryRuleConfigData(
            Context pCtx) throws RuleException {
        /*
         * Go ahead and find each element value specified in the selection criteria
         */
        Map<String, String> retMap = new HashMap<>();
        if (null != selectionCriteria && !selectionCriteria.isEmpty()) {
            Set<Map.Entry<String, SelectionCriteria>> ents = selectionCriteria.entrySet();
            for (Map.Entry<String, SelectionCriteria> sc : ents) {
                SearchResult sr = pCtx.findElement(sc.getValue().getSearchPath(),
                        sc.getValue().getFilter(),
                        sc.getValue().getTargetElements(),
                        null);
                Context c;
                if (!sr.isEmpty()) {
                    c = sr.entrySet().iterator().next().getValue();
                } else {
                    /*
                     * If not found use default value
                     */
                    String defaultVal = lookupFilterValueDefault(sc.getKey(), sc.getValue().toString(), pCtx);
                    c = ContextFactory.obtainContext(defaultVal);
                }

                if (sr.entrySet().size() > 1 || !c.isPrimitive()) {
                    throw new RuleException("Expected search results to contain only one primitive, "
                            + ", but instead found: " + c.stringRepresentation() + ". SearchCriteria was: " + sc.toString());
                }
                retMap.put(sc.getKey(), c.stringRepresentation());
            }
        }

        /*
         * Also evaluate rules (if any) to fill in rest of filter entries. If rule yields no output, try to get from
         * default (if any was configured)
         */
        if (null != selectionCriteriaForRuleEntries && !selectionCriteriaForRuleEntries.isEmpty()) {
            for (Map.Entry<String, Rule> e : selectionCriteriaForRuleEntries.entrySet()) {
                String ruleOutput = e.getValue().apply(pCtx, null, null, null, null).evaluateResultAsString();
                /**
                 * When the rule gives blank output, see if a default has been configured
                 */
                if (CommonUtils.stringIsBlank(ruleOutput) || RuleConstants.BLANK_TOKEN.equals(ruleOutput)) {
                    ruleOutput = lookupFilterValueDefault(e.getKey(), "RULE:" + e.getValue().name(), pCtx);
                }
                retMap.put(e.getKey(), ruleOutput);
            }
        }

        /**
         * Add any filter entries which are constants
         */
        if (null != selectionCriteriaConstants && !selectionCriteriaConstants.isEmpty()) {
            retMap.putAll(selectionCriteriaConstants.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
        }

        return Filter.fromMap(retMap);
    }


    /**
     * Checks to see if a default filter value was configured in the rule filter. Throws exception if there wasn't
     * a default found for passed in key.
     * @param pKey
     * @param pValSrc
     * @param pCtx
     * @return
     * @throws RuleException
     */
    private String lookupFilterValueDefault(String pKey, String pValSrc, Context pCtx) throws RuleException {
        /**
         * If not found use default value. If no default value configured,
         * throw exception to alert user of possible configuration issue.
         */
        String defaultVal;
        StringBuilder defKeySb = new StringBuilder();
        defKeySb.append(pKey);
        defKeySb.append(DEFAULT_SUFFIX);
        String defKey = defKeySb.toString();
        if (CommonUtils.stringIsBlank(defaultVal = selectionCriteriaDefaults.get(defKey))) {
            throw new RuleException("When building filter to query rule set, did not find a filter value "
                    + "in input document.\nFilter key in question is '" + pKey
                    + "'.\nConfigured filter value source is " + pValSrc + ".\nThe input document was "
                    + pCtx + ".\nEnsure you define a default value for this field in the filter map, by appending "
                    + "keyword 'default' to filter key, separated by an underscore, for example "
                    + defKey + "=<DEFAULT_VALUE_GOES_HERE>");
        }

        return defaultVal;
    }

    /**
     * Convert Map<String, String> to Map<String, SelectionCriteria>
     *
     * @param pSelectionCriteria
     * @return
     */
    private Map<String, SelectionCriteria> parseSelectionCriteria(Map<String, String> pSelectionCriteria) {
        if (null == pSelectionCriteria || pSelectionCriteria.isEmpty()) {
            return null;
        }

        return pSelectionCriteria.entrySet().stream()
                .filter(e -> !e.getValue().startsWith(PREF_RULE) && !e.getKey().endsWith(DEFAULT_SUFFIX)
                        && !e.getValue().startsWith(PREF_NO_EVAL))
                .collect(Collectors.toMap(e -> e.getKey(), e -> SelectionCriteria.valueOf(e.getValue())));
    }



    private Map<String, String> parseSelectionCriteriaDefaults(Map<String, String> pSelectionCriteria) {
        if (null == pSelectionCriteria || pSelectionCriteria.isEmpty()) {
            return null;
        }

        return pSelectionCriteria.entrySet().stream()
                .filter(e -> e.getKey().endsWith(DEFAULT_SUFFIX))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }


    /**
     * Look for filter entries which are just constants.
     * @param pSelectionCriteria
     * @return
     */
    private Map<String, String> parseSelectionCriteriaConstants(Map<String, String> pSelectionCriteria) {
        if (null == pSelectionCriteria || pSelectionCriteria.isEmpty()) {
            return null;
        }

        return pSelectionCriteria.entrySet().stream()
                .filter(e -> e.getValue().startsWith(PREF_NO_EVAL))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().substring(PREF_NO_EVAL.length())));
    }


    /**
     * Does same thing as {@link QueryableRuleConfigurationData#parseSelectionCriteria(Map)}, but for selection
     * criteria entries which value is prefixed with {@link QueryableRuleConfigurationData#PREF_RULE}, which means
     * the value for the filter entry should be dynamically populated from the value of a rule.
     * This method will simply load the rule and put it in a <code>Map</code>, keyed by the key provide
     * at configuration time.
     * @param pSelectionCriteria
     * @return
     * @throws RuleNameNotFoundException
     */
    private Map<String, Rule> parseSelectionCriteriaForRuleEntries(Map<String, String> pSelectionCriteria)
            throws RuleNameNotFoundException {
        if (null == pSelectionCriteria || pSelectionCriteria.isEmpty()) {
            return null;
        }

        /*
         * See if the selection criteria species a rule which output should be used as filter key
         * value.
         */
        Map<String, Rule> rulesMap = new HashMap<>();
        Set<Map.Entry<String, String>> entries = pSelectionCriteria.entrySet();
        for (Map.Entry<String, String> e : entries) {
            String k = e.getKey();
            String v = e.getValue();
            if (v.startsWith(PREF_RULE)) {
                rulesMap.put(k, lookupRuleByName(v.substring(PREF_RULE.length())));
            }
        }
        return rulesMap;
    }


    /**
     *
     */
    @Immutable
    static class QueryableRuleConfigurationDataResult extends QueryableDataRuleExecutionResult {
        private final String leftOperand;
        private final String operator;
        private final String rightOperand;
        private final String outputValueSuccess;
        private final String outputValueFailure;
        private final String leftOperandValueOverride;
        private final String leftOperandSource;
        private final boolean useTargetRuleOutput;
        private final String operatorOverride;
        private final int priority;


        /**
         * Note to developer: When adding a parameter that is rule set wide (I.e. applies at rule set level), then don't
         *   even bother adding to this class. only add to this class if the parameter applies at <strong>rule set operation
         *   level</strong>. Save yourself the extra, unnecessary work ;-)
         */
        static class Builder implements com.exsoinn.ie.util.Builder<QueryableRuleConfigurationDataResult> {
            // Required parameters
            private final String leftOperand;
            private final String operator;
            private final String rightOperand;

            // Optional parameters
            private boolean result = false;
            private Map<String, String> params = null;
            private Map<String, Context> searchResult = null;
            private Map<String, Rule> outputFieldMap = null;
            private Context inputContext = null;
            private String outputValueSuccess = null;
            private String outputValueFailure = null;
            private String leftOperandValueOverride = null;
            private String leftOperandSource = null;
            private boolean useTargetRuleOutput = false;
            private String operatorOverride = null;
            private int priority = 0;


            Builder(String pLeftOp, String pOp, String pRightOp) {
                leftOperand = pLeftOp;
                operator = pOp;
                rightOperand = pRightOp;
            }

            private Builder result(boolean result) {
                this.result = result;
                return this;
            }

            private Builder params(Map<String, String> params) {
                this.params = params;
                return this;
            }

            private Builder searchResult(Map<String, Context> searchResult) {
                this.searchResult = searchResult;
                return this;
            }

            private Builder outputFieldMap(Map<String, Rule> outputFieldMap) {
                this.outputFieldMap = outputFieldMap;
                return this;
            }

            public Builder inputContext(Context inputContext) {
                this.inputContext = inputContext;
                return this;
            }

            private Builder outputValueSuccess(String outputValueSuccess) {
                this.outputValueSuccess = outputValueSuccess;
                return this;
            }

            private Builder outputValueFailure(String outputValueFailure) {
                this.outputValueFailure = outputValueFailure;
                return this;
            }

            private Builder leftOperandValueOverride(String leftOperandValueOverride) {
                this.leftOperandValueOverride = leftOperandValueOverride;
                return this;
            }

            private Builder operatorOverride(String pOperatorOverride) {
                this.operatorOverride = pOperatorOverride;
                return this;
            }

            private Builder leftOperandSource(String leftOperandSource) {
                this.leftOperandSource = leftOperandSource;
                return this;
            }

            private Builder useTargetRuleOutput(boolean useTargetRuleOutput) {
                this.useTargetRuleOutput = useTargetRuleOutput;
                return this;
            }


            /**
             * Caller can set this parameter to specify a sort order. In other words this is used
             * to ensure rule set operations execute in a certain order imposed by client.
             * @param pPriority
             * @return
             */
            private Builder priority(int pPriority){
                priority = pPriority;
                return this;
            }


            @Override
            public QueryableRuleConfigurationDataResult build() {
                try {
                    return new QueryableRuleConfigurationDataResult(this);
                } catch (RuleException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        QueryableRuleConfigurationDataResult(Builder pBuilder) throws RuleException {
            super(pBuilder.result, new HashMap<>(pBuilder.params), pBuilder.searchResult, pBuilder.inputContext, pBuilder.outputFieldMap);
            leftOperand = pBuilder.leftOperand;
            operator = pBuilder.operator;
            rightOperand = pBuilder.rightOperand;
            outputValueSuccess = pBuilder.outputValueSuccess;
            outputValueFailure = pBuilder.outputValueFailure;
            leftOperandValueOverride = pBuilder.leftOperandValueOverride;
            leftOperandSource = pBuilder.leftOperandSource;
            useTargetRuleOutput = pBuilder.useTargetRuleOutput;
            operatorOverride = pBuilder.operatorOverride;
            priority = pBuilder.priority;
        }



        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("\nleftOperand=");
            sb.append(leftOperand);
            sb.append("\noperator=");
            sb.append(operator);
            sb.append("\nrightOperand=");
            sb.append(rightOperand);
            sb.append("\nleftOperandValueOverride=");
            sb.append(leftOperandValueOverride);
            sb.append("\nleftOperandSource=");
            sb.append(leftOperandSource);
            sb.append("\noperatorOverride=");
            sb.append(operatorOverride);
            return sb.toString();
        }

        String getLeftOperand() {
            return leftOperand;
        }

        String getOperator() {
            return operator;
        }

        String getRightOperand() {
            return rightOperand;
        }

        public boolean isUseTargetRuleOutput() {
            return useTargetRuleOutput;
        }

        String getOutputValueSuccess() {
            return outputValueSuccess;
        }

        String getOutputValueFailure() {
            return outputValueFailure;
        }

        String getLeftOperandValueOverride() {
            return leftOperandValueOverride;
        }

        public String getLeftOperandSource() {
            return leftOperandSource;
        }
    }


    /**
     * Encapsulates the ways in which rule operation results should be handled when a query done for a given input record
     * returns more than one rule operation to evaluate.
     */
    enum ResultEvaluationOption {
        ALL_MUST_PASS("allMustPass"),
        ANY_CAN_PASS("anyCanPass"),
        BATCH("batch"),
        ANY_CAN_PASS_EXECUTING_ALL("anyCanPassExecutingAll");

        final String resultEvalOpt;

        ResultEvaluationOption(String pResEvalOpt) {
            resultEvalOpt = pResEvalOpt;
        }

        String resultEvaluationOption() {
            return resultEvalOpt;
        }

        static ResultEvaluationOption fromResultEvaluationString(String pResEvalOpt) {
            for (ResultEvaluationOption s: ResultEvaluationOption.values()) {
                if (s.resultEvaluationOption().equals(pResEvalOpt)) {
                    return s;
                }
            }
            throw new IllegalArgumentException("Invalid/unsupported result evaluation specified, got: " +  pResEvalOpt);
        }
    }
}