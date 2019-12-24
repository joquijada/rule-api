package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.ie.util.ForwardingImmutableMap;
import com.exsoinn.util.epf.*;
import java.util.*;
import java.util.regex.Pattern;



/**
 * This class represents a rule flow which guides a record from one rule to the other via rule output of the
 * previous rule. The configuration for the rule flow is done via a {@code Context} object, created at
 * initialization time from the passed in rule configuration in a format that's currently supported (I.e.
 * JSON or XML).
 * Also all the rules that the rule flow depend on must have been previously created in the system.
 * Created by QuijadaJ on 4/19/2017.
 */
class RuleFlowImpl extends AbstractRule implements RuleFlow {
    private final RuleFlowEntity headRule;
    private static final String DELIM_OUTPUT = "|";
    private static final String DEFAULT_OUTPUT = "DEFAULT";
    private final Context ruleFlowConfig;
    private final boolean reuseContextOutput;
    private final String fieldOfInterest;


    public static class Builder extends AbstractRule.Builder<RuleFlowImpl, Builder> {
        // Required parameters
        private final Object ruleFlowConfig;

        // Optional parameters
        private boolean reuseContextOutput = false;
        private String fieldOfInterest = null;

        Builder(Object pRuleFlowConfig, String pName) {
            super(pName);
            ruleFlowConfig = pRuleFlowConfig;
        }


        /**
         * When true, it causes the output context of one rule in the flow to be the input
         * context to the rule that follows. Otherwise when false, the same context input passed
         * to {@link AbstractRule#applyBody(Context, SearchPath, Filter, TargetElements, Map)}
         * is the same context used throughout the rule flow.
         * @param pFlag
         * @return
         */
        public Builder reuseContextOutput(boolean pFlag) {
            reuseContextOutput = pFlag;
            return getThis();
        }


        /**
         * The field name specified here is picked from the output context of this rule flow,
         * and the field value can be obtained by executing {@link RuleExecutionResult#evaluateResultAsString()}
         * @param pFldName
         * @return
         */
        public Builder fieldOfInterest (String pFldName) {
            fieldOfInterest = pFldName;
            return getThis();
        }


        @Override
        Builder getThis() {
            return this;
        }


        @Override
        public RuleFlowImpl build() {
            try {
                Rule r = new RuleFlowImpl(this);
                storeInRuleByNameCache(r);
                return (RuleFlowImpl) r;
            } catch (RuleException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }


    RuleFlowImpl(Builder pBuilder) throws RuleException {
        super(pBuilder);
        ruleFlowConfig = ContextFactory.obtainContext(pBuilder.ruleFlowConfig);
        reuseContextOutput = pBuilder.reuseContextOutput;
        headRule = parseRuleMap(context().memberValue(context().startSearchPath().toString()));
        fieldOfInterest = pBuilder.fieldOfInterest;
    }



    /**
     * Build the rule objects in the flow. Below is essentially creating a forking chain, by mapping the rule outputs
     * to other rules to execute. The rules to execute are wrapped in {@link RuleFlowEntity} objects, which know how
     * to handle the rule output {@code Map} to execute the next rule.
     */
    RuleFlowEntity parseRuleMap(Context pRuleFlowCtx) throws RuleNameNotFoundException {
        /*
         * As per the way rule flow should be configured, expect that
         * only one rule name exists at any given level of the configuration data
         */
        Map.Entry<String, Context> ruleEnt = pRuleFlowCtx.entrySet().iterator().next();
        String ruleName = ruleEnt.getKey();
        Rule r = lookupRuleByName(ruleName);
        Set<Map.Entry<String, Context>> outputEnts = ruleEnt.getValue().entrySet();
        Map<String, List<RuleFlowEntity>> outputToRule = new HashMap<>();
        for (Map.Entry<String, Context> outputEnt : outputEnts) {
            List<RuleFlowEntity> rulesMappedToOutput = new ArrayList<>();
            String[] outputVal = outputEnt.getKey().split(Pattern.quote(DELIM_OUTPUT));
            Context ctxMappedToOutputVal = outputEnt.getValue();
            // For this rule create a Map of rule output to the rule to execute when that output happens. If an output maps
            // to a rule that also maps its outputs, indicated by a rule name which value is another complex object,
            // use recursion to handle that
            if (ctxMappedToOutputVal.isRecursible()) {
                rulesMappedToOutput.add(parseRuleMap(ctxMappedToOutputVal));
            } else if (ctxMappedToOutputVal.isArray()) {
                /**
                 * An output can be be mapped to a list of rules. There are restrictions though: Only rule names are allowed
                 * in the array (no rules that map to other rules, etc.). The rules will get executed in the sequence they
                 * were defined, and the rules inside the array cannot map their outputs.
                 */
                for (Context c : ctxMappedToOutputVal.asArray()) {
                    if (!c.isPrimitive()) {
                        throw new IllegalArgumentException("Problem parsing rule flow " + name() + ". An output "
                                + "mapped to an array of rules contained something other than a rule name. Currently only "
                                + " plain rule names are allowed inside arrays. Offending "
                                + " value was " + c.stringRepresentation() + "The node being process was "
                                + ctxMappedToOutputVal.stringRepresentation() + "\nFull rule flow config given was "
                                + pRuleFlowCtx.stringRepresentation());
                    }
                    Rule ruleFromAry = lookupRuleByName(c.stringRepresentation());
                    rulesMappedToOutput.add(new RuleFlowEntity.Builder(ruleFromAry, null, this)
                            .reuseContextOutput(reuseContextOutput).build());
                }
            } else {
                /**
                 * The rule mapped to this output does not map its outputs, therefore instantiate it
                 * with a NULL outputsToRule Map
                 */
                Rule nextRule = lookupRuleByName(ctxMappedToOutputVal.stringRepresentation());
                rulesMappedToOutput.add(new RuleFlowEntity.Builder(nextRule, null, this).reuseContextOutput(reuseContextOutput)
                        .build());
            }

            // Can map more than one output to the same rule
            for (String o : outputVal) {
                outputToRule.put(o, rulesMappedToOutput);
            }
        }

        return new RuleFlowEntity.Builder(r, new ForwardingImmutableMap<>(outputToRule), this)
                .reuseContextOutput(reuseContextOutput).build();
    }



    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        logDebug("**** Begin executing rule flow '" + name() + "'");
        /**
         * Important: Before rule flow execution begins, save the recorded start time because the other rules in the flow
         * also have their time start recorded by parent AbstractRule, which overwrites the main main rule flow
         * start time, since we're passing around the same pExtraParams object down to the other rules in the rule flow.
         */
        long rfStartTime = Long.valueOf(pExtraParams.get(RuleConstants.NANO_START_TIME));

        RuleExecutionResult res = headRule.apply(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams);
        Map<String, String> info = populateCommonResultProperties(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, null);
        info.put(RuleConstants.RULE_FLOW_PATH, pExtraParams.get(RuleConstants.RULE_FLOW_PATH));
        logDebug("**** End executing rule flow '" + name() + "'");

        /**
         * Replace original and correct rule flow start time for accurate performance metrics
         */
        info.put(RuleConstants.NANO_START_TIME, String.valueOf(rfStartTime));
        return new TextOutputRuleExecutionResult(true, info, res.evaluateResultAsString(), pContext, getOutputFieldMap()) {
            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(super.toString());
                sb.append("\n");
                sb.append(getParams().get(RuleConstants.RULE_FLOW_PATH));
                return sb.toString();
            }


            /**
             * Will create a {@link Context} object made of of the outputs of the rules configured in
             * {@link TextOutputRuleExecutionResult#outputFieldMap}, <strong>plus</strong> the rule flow output captured in
             * {@link TextOutputRuleExecutionResult#output}.
             * @return
             */
            @Override
            public Context evaluateResultAsContext() {
                MutableContext mc = ContextFactory.INSTANCE.obtainMutableContext("{}");
                String ruleName = getParams().get(RuleConstants.RULE_NAME);
                mc.addMember(ruleName, ContextFactory.INSTANCE.obtainContext(getOutput()));
                mc.addMember(RuleConstants.TOT_EXEC_TIME_SECS,
                        ContextFactory.INSTANCE.obtainContext(String.valueOf(totalExecutionTimeInSeconds())));
                try {
                    populateOutputData(mc);
                } catch (RuleException e) {
                    throw new IllegalArgumentException("Problem populating output fields in context. Component name is "
                            + ruleName, e);
                }
                return mc;
            }


            @Override
            public String evaluateResultAsString() throws RuleException {
                Context ctx = this.evaluateResultAsContext();
                if (!CommonUtils.stringIsBlank(fieldOfInterest)) {
                    return ctx.memberValue(fieldOfInterest).stringRepresentation();
                } else {
                    return ctx.stringRepresentation();
                }
            }
        };
    }


    @Override
    public Context context() {
        return ruleFlowConfig;
    }




    /**
     * This Rule wrapper class uses composition to provide additional functionality to a rule object, mainly to select a
     * a {@link RuleFlowEntity} based on output of this rule, and execute it, resulting in chain invocation until the path
     * in the rule flow is exhausted.
     * Is this an example of the Decorator pattern?
     */
    static class RuleFlowEntity implements Rule {
        final Map<String, List<RuleFlowEntity>> outputToRuleMap;
        final Rule ruleToExecute;
        final RuleFlow ruleFlow;
        private final boolean reuseContextOutput;

        public static class Builder extends AbstractRule.Builder<RuleFlowEntity, Builder> {
            // Required parameters
            final Map<String, List<RuleFlowEntity>> outputToRuleMap;
            final Rule ruleToExecute;
            final RuleFlow ruleFlow;

            // Optional parameters
            private boolean reuseContextOutput = false;

            public Builder(Rule pRuleToExecute,
                           Map<String, List<RuleFlowEntity>> pOutputToRuleMap,
                           RuleFlow pRuleFlow) {
                super(null);
                outputToRuleMap = pOutputToRuleMap;
                ruleToExecute = pRuleToExecute;
                ruleFlow = pRuleFlow;
            }

            Builder reuseContextOutput(boolean pFlag) {
                reuseContextOutput = pFlag;
                return getThis();
            }

            @Override
            Builder getThis() {
                return this;
            }

            @Override
            public RuleFlowEntity build() {
                return new RuleFlowEntity(getThis());
            }
        }

        RuleFlowEntity(Builder pBuilder) {
            outputToRuleMap = pBuilder.outputToRuleMap;
            ruleToExecute = pBuilder.ruleToExecute;
            ruleFlow = pBuilder.ruleFlow;
            reuseContextOutput = pBuilder.reuseContextOutput;
        }


        @Override
        public RuleExecutionResult apply(String pDataStr, String pElemSearchPath, Map<String, String> pElemFilter, Set<String> pTargetElems, Map<String, String> pExtraParams) throws RuleException {
            Context c = ContextFactory.obtainContext(pDataStr);
            return apply(c, SearchPath.valueOf(pElemSearchPath), Filter.fromMap(pElemFilter),
                    TargetElements.fromSet(pTargetElems), pExtraParams);
        }

        @Override
        public <T extends SearchPath, U extends Filter, V extends TargetElements> RuleExecutionResult
        apply(Context pContext, T pSearchPath, U pFilter, V pTargetElems, Map<String, String> pExtraParams)
                throws RuleException {
            // Execute this rule object
            RuleExecutionResult res = ruleToExecute.apply(pContext, pSearchPath, pFilter, pTargetElems, pExtraParams);

            logDebug("Rule flow '" + ruleFlow.name() + "' executed this rule: " + res.toString());

            // Evaluate the result, and see what rule to execute next, based on the output-to-rule mapping
            String output = res.evaluateResultAsString().toString();

            List<RuleFlowEntity> nextRules = new ArrayList<>();
            /*
             * Collect next rule's info, if any. If "outputToRuleMap" is null, it means this rule flow entity
             * is terminal, no other rule to execute after it
             */
            if (null != outputToRuleMap && !outputToRuleMap.isEmpty()) {
                nextRules = outputToRuleMap.get(output);

                /**
                 * Check if there's a default rule to execute when the output of this rule
                 * is not explicitly mapped
                 */
                if (null == nextRules && outputToRuleMap.containsKey(DEFAULT_OUTPUT)) {
                    nextRules = outputToRuleMap.get(DEFAULT_OUTPUT);
                }
                if (null == nextRules || nextRules.isEmpty()) {
                    throw new RuleFlowUnmappedOutputException(output, this, ruleFlow);
                }
            }

            /*
             * Have to have things in this order, so that rule flow path displays
             * the rules executed in the correct order. That's why we first add this rule to rule flow path,
             * before executing the next rule, which will also add itself to rule flow path,
             * and so on.
             */
            for (Rule r : nextRules) {
                addResultsToRuleFlowPath(pExtraParams, output, r);
            }

            if (null != nextRules && !nextRules.isEmpty()) {
                for (Rule nextRule : nextRules) {
                    /**
                     * Before next rule executes, clean up the stuff put in the shared
                     * extra params Map by the rule that just executed above. This way
                     * rules do not step over each other. Remember rules will be re-using
                     * some of the same Map key names depending on what logic each of those rules
                     * performs.
                     */
                    String flowPath = pExtraParams.get(RuleConstants.RULE_FLOW_PATH);
                    pExtraParams.clear();
                    pExtraParams.put(RuleConstants.RULE_FLOW_PATH, flowPath);
                    Context inputCtxToNextRule = pContext;
                    if (reuseContextOutput) {
                        inputCtxToNextRule = res.evaluateResultAsContext();
                    }
                    res = nextRule.apply(inputCtxToNextRule, pSearchPath, pFilter, pTargetElems, pExtraParams);
                }
            }

            return res;
        }


        private void addResultsToRuleFlowPath(Map<String, String> pExtraParams, String pRuleOutput, Rule pNextRule) {
            String flowPath = pExtraParams.get(RuleConstants.RULE_FLOW_PATH);
            StringBuilder sb = new StringBuilder();

            if (CommonUtils.stringIsBlank(flowPath)) {
                /**
                 * Super obvious incoming comment alert: When flowPath is NULL it means it's the head rule of this flow, meaning we're
                 * at the beginning of the rule flow, initiate rule flow display accordingly
                 */
                sb.append("RULE FLOW PATH [RULE FLOW NAME: ");
                sb.append(ruleFlow.name());
                sb.append("] ");
            } else {
                sb.append(flowPath);
            }

            sb.append(" --> [RULE EXECUTED: ");
            sb.append(ruleToExecute.name());
            sb.append(", OUTPUT: '");
            sb.append(pRuleOutput);
            sb.append("'");
            if (null != pNextRule) {
                sb.append(", NEXT RULE: ");
                sb.append(pNextRule.name());
            }
            sb.append("]");

            pExtraParams.put(RuleConstants.RULE_FLOW_PATH, sb.toString());
        }


        @Override
        public String name() {
            return ruleToExecute.name();
        }

        @Override
        public Map<String, Rule> getOutputFieldMap() {
            return null;
        }
    }
}



