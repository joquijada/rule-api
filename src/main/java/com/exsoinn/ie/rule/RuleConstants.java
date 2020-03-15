package com.exsoinn.ie.rule;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Non-instantiable class that holds static class fields, useful for encapsulating re-usable
 * constants in rules engine applications.
 * Created by QuijadaJ on 4/25/2017.
 */
public class RuleConstants {

    public static final String LEFT_HAND_OPERAND = "leftHandOperand";
    public static final String RIGHT_HAND_OPERAND = "rightHandOperand";
    public static final String LEFT_OPERAND_PREPROCESSOR = "leftOperandPreprocessor";
    public static final String MATCH_PATTERN = "matchPattern";
    public static final String STRING_HANDLER_VAL = "strHndlrVal";
    public static final String TARGET_VAL = "targetValue";
    public static final String SEARCH_PATH = "searchPath";
    public static final String OPERATION = "operation";
    public static final String RANGE = "range";
    public static final String RULE_ID = "ruleId";
    public static final String RULE_CLASS = "ruleClass";
    public static final String RULE_OBJECT = "ruleObject";
    public static final String RULE_NAME = "ruleName";
    public static final String QUERY_SEARCH_PATH = "querySearchPath";
    public static final String QUERY_RETURN_ELEMENTS = "queryTargetElements";
    public static final String QUERY_FILTER = "queryFilter";
    public static final String JAVA_SCRIPT_SRC_FOLDER_NAME = "js/";
    public static final String RULE_TYPE_PREFIX_REGEX = "REGEX";
    public static final String RULE_TYPE_PREFIX_LOOKUP = "LU";
    public static final String EXPLICIT_RULE_EVAL_TARGET = "explicitRuleEvaluationTarget";
    public static final String NANO_START_TIME = "startTimeInNano";
    public static final String NANO_END_TIME = "endTimeInNano";
    public static final String TOT_EXEC_TIME_SECS = "totalExecutionTimeInSeconds";
    public final static int IGNORE_INT = -9999;
    public static final String VAL_TO_CHECK = "valueToCheck";
    public static final String RULE_FLOW_PATH = "ruleFlowPath";
    public static final String MATCH_STYLE = "matchStyle";
    public static final String BLANK_TOKEN = "__BLANK__";
    public static final String INPUT_SET_DELIM = "__INPUT_SET__";
    public static final String INPUT_ELEMS_DELIM = "--";
    public static final String NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String LOOKUP_OUTPUT_VALUE = "lookupOutputValue";
    public final static String PREF_RULE = "RULE:";
    public final static String PREF_NO_EVAL = "NO-EVAL:";
    public final static String MULTI_VAL_STR_DELIM = ",";
    public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat( "MM/dd/yyyy" );
    public final static String RULE_EXPRESSION = "ruleExpression";
    public static final String CURRENT_CONTEXT = "currentContext";
    public static final String SUPPRESS_CONTEXT = "suppressContext";





    /*
     * Constants for the keys to use in argument parameters, a separate set is declared to
     * prevent collisions with like keys used for other purposes. The constants defined below
     * are to be used *only* in the extra param Map passed in applyBody() method
     */
    public static final String ARG_LEFT_OPERAND = "argLeftOperand";
    public static final String ARG_LEFT_OPERAND_SRC = "argLeftOperandSource";
    public static final String ARG_RIGHT_OPERAND = "argRightOperand";
    public static final String ARG_OPERATION = "argOperation";
    public static final String ARG_RANGE = "argRange";
    public static final String ARG_VAL_TO_CHECK = "argValueToCheck";
    public static final String ARG_REGEX_TARGET_VAL = "argRegExTargetValue";
    public static final String ARG_REGEX_PATTERN = "argRegExPattern";
    public static final String ARG_LIST_NAME = "argListName";
    public static final String ARG_LOOKUP_COMPONENT_NAME = "argLookupComponentName";
    public static final String ARG_ORIGINAL_CONTEXT = "argOriginalContext";
    public static final String ARG_PARENT_CONTEXT = "argParentContext";


    /*
     * Use the below constant to pass the name of a rule that will be executed by another rule object. An
     * example of this is ConfigurableRuleExecutor, which accepts the name of a configurable rule to execute.
     */
    public static final String ARG_TARGET_RULE_NAME = "argTargetRuleName";


    //Content Validation Constants
    public static final String STR_HANDLER_OPR = "STR_HANDLER";


    /**
     * Configuration property name constants. These are configuration parameters that a
     * a child class of {@link AbstractRulesEngine} can set by invoking in that child class, the
     * {@link AbstractRulesEngine#addConfigurationProperties} method.
     */
    public static final String CONFIG_PROP_TASK_TIMEOUT_SECONDS = "taskTimeoutSeconds";
    public static final String CONFIG_PROP_LU_CONTEXT_PARTITION_THRESHOLD = "lookupContextPartitionThreshold";
    public static final String CONFIG_PROP_LU_CONTEXT_PARTITION_SIZE = "lookupContextPartitionSize";
    public static final String CONFIG_PROP_THREAD_POOL_SIZE = "threadPoolSize";
    public static final String CONFIG_PROP_PARALLEL_RULE_EXEC = "parallelizedRuleExecution";
    public static final String CONFIG_PROP_LIST_CTX_ROW_CNT_CACHE_THRESHOLD = "listContextRowCountCacheThreshold";


    /*
     * Suppresses default constructor, ensuring non-instantiability.
     */
    private RuleConstants() {

    }
}
