package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.ie.util.concurrent.SequentialTaskExecutor;
import com.exsoinn.util.DnbBusinessObject;
import com.exsoinn.util.epf.*;
import net.jcip.annotations.Immutable;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Created by QuijadaJ on 5/18/2017.
 */
class ConfigurableRuleExecutor extends AbstractRule implements RuleExecutor<QueryableRuleConfigurationData> {
    final static String GENERIC_CACHE_KEY = "genericRuleConfigurationExecutor";
    private final static List<QueryableRuleConfigurationData.ResultEvaluationOption> supportedEvalOpts;

    static {
        supportedEvalOpts = new ArrayList<>();
        supportedEvalOpts.add(QueryableRuleConfigurationData.ResultEvaluationOption.ALL_MUST_PASS);
        supportedEvalOpts.add(QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS);
        supportedEvalOpts.add(QueryableRuleConfigurationData.ResultEvaluationOption.BATCH);
        supportedEvalOpts.add(QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS_EXECUTING_ALL);
    }

    /*
     * Constructors
     */
    ConfigurableRuleExecutor(String pUniqueName) {
        super(pUniqueName);
    }

    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {
        /*
         * Crucial step one, which is to load the rule operations that are applicable
         * to this input record. The inpt record is stored in the Context object passed into this
         * method as the pContext argument.
         */
        final Rule foundRule = lookupRuleByName(pExtraParams.get(RuleConstants.ARG_TARGET_RULE_NAME));

        if (!(foundRule instanceof QueryableRuleConfigurationData)) {
            throw new RuleException(this.getClass().getName() + " only handles components of type "
                    + QueryableRuleConfigurationData.class.getName() + ", and was given rule of type "
                    + foundRule.getClass().getName() + ". The name of the offending rule was " + foundRule.name());
        }

        final QueryableRuleConfigurationData qrcd = (QueryableRuleConfigurationData) foundRule;
        List<QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult> qryConfigList;
        boolean interrupted = false;
        try {
            qryConfigList = qrcd.obtainRuleConfig(pContext, pExtraParams);
            if (null == qryConfigList) {
                return null;
            }
        } catch (InterruptedException e) {
            interrupted = true;
            throw new RuleException(e);
        } finally {
            if (interrupted) {
                // Restore thread interrupted status so that thread owning code can handle appropriately
                Thread.currentThread().interrupt();
            }
        }
        QueryableRuleConfigurationData.ResultEvaluationOption resEvalOpt = qrcd.getResultEvaluationOption();
        if (!supportedEvalOpts.contains(resEvalOpt)) {
            throw new RuleException("Unsupported configurable rule evaluation option: " + resEvalOpt.toString());
        }

        /**
         * Build a list of Callable's, each Callable represents a rule operation
         * to execute, returning a {@link ExecutedConfigurableRuleInfo} upon successfule
         * execution.
         */
        Collection<Callable<ExecutedConfigurableRuleInfo>> ruleTasks = new ArrayList<>();
        List<ExecutedConfigurableRuleInfo> ruleOpResults = new CopyOnWriteArrayList<>();
        for (QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult q : qryConfigList) {
            RuleAndParameterMapWrapper rpw = buildRule(q, qrcd);
            ruleTasks.add(() -> {
                RuleExecutionResult ruleRes = rpw.rule.apply(pContext, null, null, null, rpw.parameters);
                ExecutedConfigurableRuleInfo execResInfo = new ExecutedConfigurableRuleInfo(q, ruleRes);

                /*
                 * The list below is needed for case when "ALL must pass" and "Batch", or for "ANY can pass" scenario and
                 * no rule op passed evaluation. For "ALL must pass", after all the tasks execute, we need
                 * to evaluate ALL the results to check if all of them passed. For the latter we need
                 * in case no rule op passed, in which case there will be NULL result returned, and the only way to obtain
                 * the results in such a case is by retrieving from "ruleOpResults" list.
                 */
                ruleOpResults.add(execResInfo);
                return execResInfo;
            });
        }


        /**
         * Now execute all the rule op Callable's built above. Depending on rule evaluation option we pick
         * a different execution strategy:
         * "ANY can pass" - Execute in parallel, but cancel execution as soon as first passing rule op is encountered
         * "ALL must pass" - Execute in parallel as well, but wait for ALL rule ops to finish completion. We need to see all
         *   results to know if ALL passed or not (it's all-or-nothing)
         *
         * Note: The "ecri" variable is reserved for "ANY can pass" scenario only. "ALL must pass" will retrieve results
         *   from "ruleOpResults". That's because the former uses a different execution policy, and because of they way
         *   Java Runnable and Future API's differ: Runnable does not return a result, but Future does. And when "ANY can
         *   pass" scenario happens, we're interested in only one result, and for "ALL must pass" when a want a
         *   *list* of results. We could have attempted to modify {@link CommonUtils#runTasksAsynchronouslyAndCancelOnFirstResult(ExecutorService, Collection)}
         *   to always return a list, but that path proved to be more difficult.
         */
        boolean wasInterrupted = false;
        Throwable t = null;
        ExecutedConfigurableRuleInfo ecri = null;
        final ExecutorService es;
        /**
         * Do not even attempt to execute rules in parallel unless the rule set was specifically
         * configured to allow such thing. Why? Some rule sets out there might depend on the order
         * of execution of the rule set operations, in which case executing things in parallel can throw
         * a monkey wrench, given the asynchronous nature of execution that parallelization introduces. The
         * flag {@link QueryableRuleConfigurationData#isCanExecuteInParallel()} tells if things can
         * be executed in parallel or not
         * TODO: Mention examples of rule configs out there that rely on order of execution of rule set ops. Can
         * TODO: overcome this by assigning a priority to each rule op.
         */
        if (!AbstractRulesEngine.parallelizedRuleExecution() || !qrcd.isCanExecuteInParallel()) {
            logDebug("Ready to execute tasks, will execute " + ruleTasks.size() + " ***sequentially***.");
            es = SequentialTaskExecutor.SEQUENTIAL_TASK_EXECUTOR;
        } else {
            logDebug("Ready to execute tasks, will execute " + ruleTasks.size() + "tasks, each in its own thread.");
            es = CommonUtils.autoSizedExecutor();
        }
        try {
            /**
             * When any can pass, return as soon as we find the first operation that evaluates to true. The
             * {@link CommonUtils#runTasksAsynchronouslyAndCancelOnFirstResult(ExecutorService, Collection)}  will
             * take care of that for us. Any remaining tasks get cancelled whether running or not.
             * When all must pass, we need *all* the results back, and then we examine each result further below to see
             * if indeed all results evaluate to true. Batch and "any can pass executing all" share in common with "all
             * must pass" the fact that all rule operations in the set must be executed.
             */
            if (resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS) {
                ecri = CommonUtils.runTasksAsynchronouslyAndCancelOnFirstResult(es, ruleTasks);
            } else if (resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ALL_MUST_PASS
                    || resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.BATCH
                    || resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS_EXECUTING_ALL) {
                Collection<FutureTask<ExecutedConfigurableRuleInfo>> ftList =
                        ruleTasks.stream().map(FutureTask::new).collect(Collectors.toList());
                t = CommonUtils.runOperationsAsynchronouslyAndWaitForAllToComplete(
                        es, ftList, AbstractRulesEngine.obtainTaskTimeoutSeconds());
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                wasInterrupted = true;
            }
            throw new RuleException("There was a problem executing configurable rule " + qrcd.name() + ": ", e);
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }

        if (null != t) {
            throw new RuleException("There was an error executing configurable rule " + qrcd.name(), t);
        }


        /**
         * Select the pertinent rule op result depending on the rule set evaluation, that will
         * get used to get success/failure override output (if any configured in the rule set)
         * configured.
         */
        ExecutedConfigurableRuleInfo selectedEcri = null;
        if (resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS) {
            if (null != ecri) {
                /**
                 * It was an "ANY can pass" config rule evaluation, and at least one
                 * rule op passed, this is our selected result, because the async tasks were cancelled upon the first
                 * rule set op that evaluated to true, and that was the captured rule op result.
                 * If NULL, it means NONE passed, in which case further below will get last result added to list by the
                 * async tasks.
                 */
                selectedEcri = ecri;
            }
        } else {
            Optional<ExecutedConfigurableRuleInfo> ruleResult;
            /**
             * Request was to execute all, and regard rule set as successful as long
             * as at least one of the executed rule set operations passed
             */
            if (resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS_EXECUTING_ALL) {
                ruleResult = ruleOpResults.stream().filter(e -> e.executionResult.evaluateResult()).findFirst();
            } else {
                /**
                 * It was "ALL must pass" config rule evaluation, take a look at each result and ensure
                 * ALL evaluated to true
                 */
                ruleResult = ruleOpResults.stream().filter(e -> !e.executionResult.evaluateResult()).findFirst();
            }

            /**
             * When "ruleResult" contains a value, it means one or more rule ops evaluated to true or false, depending respectively
             * on ALL or ANY pass eval option. Get the first
             * such occurrence. Else just get the last result added to the list by the async tasks (done farther below).
             */
            if (ruleResult.isPresent()) {
                selectedEcri = ruleResult.get();
            }
        }

        /**
         * A NULL "selectedEcri" has a different meaning depending on rule evaluation option chosen
         * "ANY can pass" - If NULL result, it means NON passed
         * "ALL must pass" - If NULL result, it means ALL passed (no failed rule op found, see above). If that's
         * the case just grab the first result from the list.
         * "Batch"/"ANY can pass executing all" - Similar to "ALL must pass". What these modes have in common is that
         *   they cause unconditional execution of all rule operations in the rule set. The
         *   {@link ConfigurableRuleExecutorResult} gives appropriate treatment to these options when putting together
         *   the results that are given back to client.
         * Note: We code this way to avoid replicating lines of code. For example we didn't want to replicate
         *   "ecriList.get(ecriList.size() - 1)" above.
         */
        if (null == selectedEcri) {
            selectedEcri = ruleOpResults.get(ruleOpResults.size() - 1);
        }

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, null);

        String outputVal = calculateOutputValue(selectedEcri);
        boolean outcome = selectedEcri.executionResult.evaluateResult();
        return new ConfigurableRuleExecutorResult.Builder(outcome, outputVal, pContext,
                QueryableRuleConfigurationData.ResultEvaluationOption
                        .fromResultEvaluationString(resEvalOpt.resultEvaluationOption()))
                .params(info)
                .selectedResult(selectedEcri)
                .outputFieldMap(getOutputFieldMap())
                .executedConfigurableRuleInfo(ruleOpResults)
                .additionalContextFields((ContextAugmentingRule) qrcd.getAdditionalContextFields())
                .printAllResults(resEvalOpt == QueryableRuleConfigurationData.ResultEvaluationOption.ANY_CAN_PASS_EXECUTING_ALL)
                .copyToCurrentContext(qrcd.getCopyToCurrentContext())
                .build();
    }



    /**
     * Fetch override success/failure output strings (if any were configured in the rule set). By default,
     * these values will be "true" and "false" respectively, depending on whether rule outcome was success or not.
     * See {@link TextOutputRuleExecutionResult#evaluateResultAsString()} to see the logic that displays
     * true/false default when no text value was given in constructor.
     * @param pExecRuleConfigInfo
     * @return
     */
    private static String calculateOutputValue(ExecutedConfigurableRuleInfo pExecRuleConfigInfo) {
        String outputVal = null;
        String outValSucc = pExecRuleConfigInfo.ruleConfiguration.getOutputValueSuccess();
        String outValFail = pExecRuleConfigInfo.ruleConfiguration.getOutputValueFailure();
        boolean outcome = pExecRuleConfigInfo.executionResult.evaluateResult();
        if (outcome && !CommonUtils.stringIsBlank(outValSucc)) {
            outputVal = outValSucc;
        } else if (!outcome && !CommonUtils.stringIsBlank(outValFail) ) {
            outputVal = outValFail;
        }

        return outputVal;
    }



    @Immutable
    static class ExecutedConfigurableRuleInfo implements DnbBusinessObject{
        final QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult ruleConfiguration;
        final RuleExecutionResult executionResult;

        ExecutedConfigurableRuleInfo(
                QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult pRuleConfig,
                RuleExecutionResult pExecRes) {
            ruleConfiguration = pRuleConfig;
            executionResult = pExecRes;
        }

        public QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult getRuleConfiguration() {
            return ruleConfiguration;
        }

        public RuleExecutionResult getExecutionResult() {
            return executionResult;
        }

        /**
         * If the result is true, say that it is *not* empty.
         * @return
         */
        @Override
        public boolean isEmpty(){
            return !executionResult.evaluateResult();
        }
    }




    @Immutable
    static class ConfigurableRuleExecutorResult extends TextOutputRuleExecutionResult
            implements ExecutorRuleExecutionResult {
        private final List<ExecutedConfigurableRuleInfo> executedConfigurableRuleInfo;
        private final ExecutedConfigurableRuleInfo selectedResult;
        private final QueryableRuleConfigurationData.ResultEvaluationOption evalOption;
        private final Rule additionalContextFields;
        private final boolean printAllResults;
        private final Map<String, String> copyToCurrentContext;

        private final static String LBL_OPS_EXECUTED = "operationsExecuted";
        private final static String LBL_EXEC_TIME = "executionTimeInSeconds";

        static class Builder implements com.exsoinn.ie.util.Builder<ConfigurableRuleExecutorResult> {
            // Required parameters
            private final boolean result;
            private final String output;
            private final Context inputContext;
            private final QueryableRuleConfigurationData.ResultEvaluationOption evalOption;

            // Optional parameters
            private Map<String, String> params;
            private Map<String, Rule> outputFieldMap;
            private List<ExecutedConfigurableRuleInfo> executedConfigurableRuleInfo;
            private ExecutedConfigurableRuleInfo selectedResult;
            private Rule additionalContextFields = null;
            private boolean printAllResults = false;
            private Map<String, String> copyToCurrentContext = null;


            Builder(boolean pRes, String pOutput, Context pInputCtx,
                    QueryableRuleConfigurationData.ResultEvaluationOption pEvalOption) {
                result = pRes;
                output = pOutput;
                inputContext = pInputCtx;
                evalOption = pEvalOption;
            }

            private Builder outputFieldMap(Map<String, Rule> pMap) {
                outputFieldMap = pMap;
                return this;
            }

            private Builder params(Map<String, String> params) {
                this.params = params;
                return this;
            }

            private Builder executedConfigurableRuleInfo(List<ExecutedConfigurableRuleInfo> executedConfigurableRuleInfo) {
                this.executedConfigurableRuleInfo = executedConfigurableRuleInfo;
                return this;
            }

            private Builder selectedResult(ExecutedConfigurableRuleInfo selectedResult) {
                this.selectedResult = selectedResult;
                return this;
            }

            private <T extends ContextAugmentingRule> Builder additionalContextFields(T pRule) {
                additionalContextFields = pRule;
                return this;
            }


            private Builder printAllResults(boolean pFlag) {
                printAllResults = pFlag;
                return this;
            }

            public Builder copyToCurrentContext(Map<String, String> pFlds) {
                copyToCurrentContext = pFlds;
                return this;
            }


            @Override
            public ConfigurableRuleExecutorResult build() {
                try {
                    return new ConfigurableRuleExecutorResult(this);
                } catch (RuleException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }


        ConfigurableRuleExecutorResult(Builder pBuilder) throws RuleException {
            super(pBuilder.result, pBuilder.params, pBuilder.output, pBuilder.inputContext, pBuilder.outputFieldMap);
            executedConfigurableRuleInfo = pBuilder.executedConfigurableRuleInfo;
            selectedResult = pBuilder.selectedResult;
            evalOption = pBuilder.evalOption;
            additionalContextFields = pBuilder.additionalContextFields;
            printAllResults = pBuilder.printAllResults;
            copyToCurrentContext = pBuilder.copyToCurrentContext;
        }



        /**
         * If possible, get a {@code Context} object that encapsulates all rule results fields that the
         * rule wishes to capture. But not all rule result classes
         * implement this method (yet), in which case it will throw {@link UnsupportedOperationException}, but we capture it
         * and simply re-throw it.
         * @return - A {@link Context} object with pertinent rule result information, in the form of name/value pairs
         */
        @Override
        public Context evaluateResultAsContext() {
            /**
             * Batch mode means that all rule set operations get executed unconditionally, producing an array of JSON's
             * of these results.
             */
            Context retCtx;
            if (evalOption == QueryableRuleConfigurationData.ResultEvaluationOption.BATCH) {
                MutableContext opsList = ContextFactory.obtainMutableContext("[]");
                for (ExecutedConfigurableRuleInfo eri : executedConfigurableRuleInfo) {
                    MutableContext opMc = ContextFactory.obtainMutableContext(
                            buildContext(eri, true).stringRepresentation());
                    addNameValuePairToContext(LBL_EXEC_TIME, String.valueOf(eri.executionResult.totalExecutionTimeInSeconds()),
                            opMc);
                    Context newCtx = opMc;
                    if (null != additionalContextFields) {
                        try {
                            newCtx = addFieldsToContext(opMc, (ContextAugmentingRule) additionalContextFields);
                        } catch (RuleException e) {
                            throw new IllegalArgumentException("Problem adding additional fields to output context. Target rule "
                                    + "name is " + getParams().get(RuleConstants.ARG_TARGET_RULE_NAME), e);
                        }
                    }
                    opsList.addEntryToArray(newCtx);
                }
                MutableContext mc = ContextFactory.obtainMutableContext("{}");
                mc.addMember(LBL_OPS_EXECUTED, opsList);
                addNameValuePairToContext("totalExecutionTimeInSeconds", String.valueOf(totalExecutionTimeInSeconds()), mc);
                retCtx = ContextFactory.obtainContext(mc.stringRepresentation());
            } else {
                Context ctx = buildContext(selectedResult, false);
                if (null != additionalContextFields) {
                    try {
                        ctx = addFieldsToContext(ctx, (ContextAugmentingRule) additionalContextFields);
                    } catch (RuleException e) {
                        throw new IllegalArgumentException("Problem adding additional fields to output context. Target rule "
                                + "name is " + getParams().get(RuleConstants.ARG_TARGET_RULE_NAME), e);
                    }
                }
                retCtx = ctx;
            }

            /**
             * When parameter copyToCurrentContext has been specified, the context which just got evaluated (I.e.
             * current context) gets augmented with the fields listed in copyToCurrentContext, and the values are
             * fetched from the context produced by this rule set operation.
             * When eval option is batch, which means an array of objects is generated (one per lookup executed), then
             * the fields are copied from each of those objects into brand new objects, out of which a new array is
             * generated and *added* to the current context. The array is stored under generic member name "result"
             */
            if (null != copyToCurrentContext && !copyToCurrentContext.isEmpty()) {
                MutableContext curMutCtx =
                        ContextFactory.obtainMutableContext(getParams().get(RuleConstants.CURRENT_CONTEXT));
                if (evalOption == QueryableRuleConfigurationData.ResultEvaluationOption.BATCH) {
                    MutableContext aryCtx = ContextFactory.obtainMutableContext("[]");
                    for (Context c : retCtx.memberValue(LBL_OPS_EXECUTED).asArray()) {
                        MutableContext targetCtx = ContextFactory.obtainMutableContext("{}");
                        Context newCtx = copyFields(c, targetCtx, copyToCurrentContext);
                        aryCtx.addEntryToArray(newCtx);
                    }
                    curMutCtx.addMember("result", aryCtx);
                    retCtx = ContextFactory.obtainContext(curMutCtx.stringRepresentation());
                } else {
                    retCtx = ContextFactory.obtainContext(
                            copyFields(retCtx, curMutCtx, copyToCurrentContext).stringRepresentation());
                }
            }

            return retCtx;
        }

        /**
         * Helper method to copy fields from one complex Context to another, the new Context gets returned. Once a
         * field gets added, it won't be overridden; in those cases the other occurrences of the field get skipped.
         * @param pFromCtx
         * @param pToCtx
         * @param pFlds
         * @return
         */
        private Context copyFields(Context pFromCtx, Context pToCtx, Map<String, String> pFlds) {
            if (!pFromCtx.isRecursible()) {
                throw new IllegalArgumentException("The 'from' Context must be a single complex object in order to copy,"
                        + " context was " + pFromCtx);
            }

            if (!pFromCtx.isRecursible()) {
                throw new IllegalArgumentException("The 'to' Context must be a single complex object in order to copy,"
                        + " context was " + pToCtx);
            }

            MutableContext mc =
                    ContextFactory.obtainMutableContext(pToCtx.stringRepresentation());
            for (Map.Entry<String, String> e : pFlds.entrySet()) {
                mc.addMember(e.getValue(), pFromCtx.memberValue(e.getKey()));
            }
            return ContextFactory.obtainContext(mc.stringRepresentation());

        }




        /**
         * Helper method to generate the executed rule output.
         *
         * @param pExecInfo
         * @param pBatchMode
         * @return
         */
        private Context buildContext(ExecutedConfigurableRuleInfo pExecInfo, boolean pBatchMode) {
            try {
                Context resCtx = pExecInfo.executionResult.evaluateResultAsContext(!pBatchMode);
                MutableContext mc = ContextFactory.obtainMutableContext(resCtx.stringRepresentation());
                String output;
                if (pBatchMode) {
                    boolean useTargetRuleOutput = pExecInfo.ruleConfiguration.isUseTargetRuleOutput();
                    /**
                     * Are we using the target rule's output, or this executor's output? The
                     * {@link ConfigurableRuleExecutorResult#getOutput()} in the "else" takes care of
                     * examining {@link QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult#isUseTargetRuleOutput()}.
                     */
                    if (useTargetRuleOutput) {
                        output = pExecInfo.executionResult.getOutput();
                    } else {
                        output = calculateOutputValue(pExecInfo);
                    }
                } else {
                    output = getOutput();
                }
                addNameValuePairToContext("output", output, mc);
                addNameValuePairToContext("executionTimeInSeconds", String.valueOf(totalExecutionTimeInSeconds()), mc);

                /**
                 * When the selected rule operation result evaluates to false (and not in batch mode), or when it has
                 * been requested explicitly to output all the rule operations executed, include a list
                 * of all operations performed. This helps with tracking and auditing,
                 * and makes application functioning transparent to the user.
                 */
                if ((!pExecInfo.executionResult.evaluateResult() && !pBatchMode) || printAllResults) {
                    MutableContext opsList = ContextFactory.obtainMutableContext("[]");
                    for (ExecutedConfigurableRuleInfo ecri : executedConfigurableRuleInfo) {
                        boolean useTargetRuleOutput = ecri.ruleConfiguration.isUseTargetRuleOutput();
                        MutableContext targetRuleMc =
                                ContextFactory.obtainMutableContext(ecri.executionResult.evaluateResultAsContext(false).toString());
                        String opOutput;
                        if (useTargetRuleOutput) {
                            opOutput = ecri.executionResult.getOutput();
                        } else {
                            opOutput = calculateOutputValue(ecri);
                        }
                        addNameValuePairToContext("output", opOutput, targetRuleMc);
                        opsList.addEntryToArray(ContextFactory.obtainContext(targetRuleMc.stringRepresentation()));
                    }
                    mc.addMember(LBL_OPS_EXECUTED, opsList);
                }
                return ContextFactory.obtainContext(mc.stringRepresentation());
            } catch (Throwable e) {
                /*
                 * Simply rethrow it, as not all rule result classes implement
                 * this method.
                 */
                throw e;
            }
        }


        /**
         * Overridden to see if the output of the selected rule from the ones executed by this {@link ConfigurableRuleExecutor} should
         * be the one returned to the caller, namely by checking
         * {@link QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult#isUseTargetRuleOutput()}.
         * Else just use this {@link ConfigurableRuleExecutor}'s own output, which usually
         * is the success or failure (depending on outcome of rule operation evaluation) configured in the rule set that this
         * {@link ConfigurableRuleExecutor} has executed.
         * @return
         */
        @Override
        public String getOutput() {
            if (selectedResult.getRuleConfiguration().isUseTargetRuleOutput()) {
                return selectedResult.executionResult.getOutput();
            } else {
                return super.getOutput();
            }
        }

        /**
         * Overridden for same reason(s) as <code>getOutput}</code> defined in this class, see that method comments
         * for details.
         * @return
         * @throws RuleException
         */
        @Override
        public String evaluateResultAsString() throws RuleException {
            if (selectedResult.getRuleConfiguration().isUseTargetRuleOutput()) {
                return selectedResult.executionResult.getOutput();
            } else {
                return super.evaluateResultAsString();
            }
        }

        @Override
        public List<RuleExecutionResult> getExecutedRuleResults() {
            return executedConfigurableRuleInfo.stream().map(ExecutedConfigurableRuleInfo::getExecutionResult)
                    .collect(Collectors.toList());
        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("\n\nRule Operations Executed:");
            for (ExecutedConfigurableRuleInfo ecri : executedConfigurableRuleInfo) {
                //sb.append("\nRule Configuration Info:");
                //sb.append(ecri.ruleConfiguration.toString());
                sb.append("\nRule Result:");
                sb.append(ecri.executionResult.toString());
                sb.append("\n------------------------");
            }
            try {
                sb.append("\nFinal Rule Set Result is " + evaluateResultAsString());
            } catch (RuleException e) {
                logError(e);
            }
            sb.append("\n***** END of rule ");
            sb.append(getParams().get(RuleConstants.RULE_NAME));
            sb.append(" *****\n\n");
            return sb.toString();
        }
    }


    /**
     * Based on operator obtained from rule configuration, instantiate the correct rule component to execute
     * the operation and obtain a result, and put add the name/values expected
     *
     * @param pRuleConfigQryRes
     * @return
     * @throws RuleException
     */
    private RuleAndParameterMapWrapper buildRule(
            QueryableRuleConfigurationData.QueryableRuleConfigurationDataResult pRuleConfigQryRes,
            QueryableRuleConfigurationData pConfigRule)
            throws RuleException {
        Rule r;
        RuleType rt = RuleType.fromSymbol(pRuleConfigQryRes.getOperator());
        Map<String, String> params = new HashMap<>();

        switch(rt) {
            case RegExRuleType:
                r = AbstractRule.createGenericRegExRule();
                params.put(RuleConstants.ARG_REGEX_PATTERN, pRuleConfigQryRes.getRightOperand());
                params.put(RuleConstants.ARG_REGEX_TARGET_VAL, pRuleConfigQryRes.getLeftOperand());
                break;
            case RelOpRuleType:
                r = AbstractRule.createGenericRelOpRule();
                params.put(RuleConstants.ARG_LEFT_OPERAND, pRuleConfigQryRes.getLeftOperand());
                params.put(RuleConstants.ARG_OPERATION, pRuleConfigQryRes.getOperator());
                params.put(RuleConstants.ARG_RIGHT_OPERAND, pRuleConfigQryRes.getRightOperand());
                break;
            case CheckAgainstCollectionRuleType:
                r = AbstractRule.createGenericCheckAgainstCollectionRule();
                params.put(RuleConstants.ARG_RANGE, pRuleConfigQryRes.getRightOperand());
                params.put(RuleConstants.ARG_OPERATION, pRuleConfigQryRes.getOperator());
                params.put(RuleConstants.ARG_VAL_TO_CHECK, pRuleConfigQryRes.getLeftOperand());
                break;
            case LookupRuleType:
                String[] tokens;
                String luRuleName = pRuleConfigQryRes.getRightOperand();

                if (luRuleName.contains("-")) {
                    tokens = luRuleName.split("-", 2);
                } else {
                    throw new RuleException("Lookup rule type does not comply to the format. Eg. \"ruleName-ruleType\"");
                }

                String componentName = tokens[0];
                String listName = tokens[1];
                /*
                 * Transfer any output fields in the configurable rule to the target rule component
                 * to be executed. This might need to be done for other rule component flavors as well,
                 * on an as-needed basis.
                 * Need to transform the already put together Map of key-to-Rule so that
                 * I can effectively use the {@link AbstractRule#AbstractRule(String, List)} constructor. That
                 * constructor will then turn it back to the original <code>Map</code>.
                 */
                List<String> l = null;
                if (null != pConfigRule.getOutputFieldMap()) {
                    l = pConfigRule.getOutputFieldMap().entrySet().stream()
                            .map(e -> e.getValue().name() + "||" + e.getKey()).collect(Collectors.toList());
                }
                StringBuilder sb = new StringBuilder(componentName);
                sb.append("-");
                sb.append(listName);
                r = lookupRuleExecutorBuilder(sb.toString())
                        .outputFields(l)
                        .ignoreListNotExistsError(pConfigRule.isIgnoreListNotExistsError())
                        .build();
                params.put(RuleConstants.ARG_LOOKUP_COMPONENT_NAME, componentName);
                params.put(RuleConstants.ARG_LIST_NAME, listName);
                params.put(RuleConstants.ARG_OPERATION, pRuleConfigQryRes.getOperator());
                params.put(RuleConstants.ARG_LEFT_OPERAND, pRuleConfigQryRes.getLeftOperand());
                params.put(RuleConstants.ARG_LEFT_OPERAND_SRC, pRuleConfigQryRes.getLeftOperandSource());
                break;
            case StringHandlerRuleType:
                String[] token = pRuleConfigQryRes.getOperator().split(Pattern.quote("||"));

                if (null != token && token.length > 2) {
                    /*
                     * Doing both a string op and a string pre-processing operations.
                     */
                    r = AbstractRule.createStringHandlerRule(token[1], token[2]);
                } else if (null != token && token.length == 2) {
                    /*
                     * Doing only a straight up string operation, no string pre-processing.
                     */
                    r = AbstractRule.createStringHandlerRule(token[1]);
                } else {
                    throw new RuleException("String Handler rule type does not comply to the format. "
                            + "Eg. \"STR_HANDLER||EQUALS||UPPER_CASE\", or "
                            + "\"STR_HANDLER||IS_NULL\"");
                }
                params.put(RuleConstants.ARG_LEFT_OPERAND, pRuleConfigQryRes.getLeftOperand());
                params.put(RuleConstants.ARG_OPERATION, pRuleConfigQryRes.getOperator());
                params.put(RuleConstants.ARG_RIGHT_OPERAND, pRuleConfigQryRes.getRightOperand());

                break;
            default:
                throw new RuleException("Rule type not supported for rule object creation: " + rt);
        }

        return new RuleAndParameterMapWrapper(r, params);
    }


    @Immutable
    private static class RuleAndParameterMapWrapper {
        final Rule rule;
        final Map<String, String> parameters;

        RuleAndParameterMapWrapper(Rule pRule,
                                   Map<String, String> pParams) {
            rule = pRule;
            parameters = pParams;
        }
    }

    /*
     * Encapsulates the rule types which support having operands/operators being dynamically
     * populated at runtime from a data source
     */
    enum RuleType {
        RegExRuleType,
        RelOpRuleType,
        CheckAgainstCollectionRuleType,
        LookupRuleType,
        StringHandlerRuleType;


        static RuleType fromSymbol(String pOp) {

            if(pOp.indexOf(RuleConstants.STR_HANDLER_OPR) == 0) {
                return StringHandlerRuleType;
            } else if (pOp.indexOf(RuleConstants.RULE_TYPE_PREFIX_REGEX) == 0) {
                return RegExRuleType;
            } else {
                /*
                 * Try different rule components to find a suitable type that can handle this operation. When
                 * success is encountered, we'll return out of this method right away. Otherwise the very end
                 * will throw exception.
                 */
                try {
                    if (null != RelOpRule.Operation.fromSymbol(pOp)) {
                        return RelOpRuleType;
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore, it will be thrown at end of method
                }
                try {
                    if (null != CheckAgainstCollectionRule.Operation.fromSymbol(pOp)) {
                        return CheckAgainstCollectionRuleType;
                    }
                } catch (IllegalArgumentException e) {
                    // Ignore, it will be thrown at end of method
                }
                try {
                    if (pOp.indexOf(RuleConstants.RULE_TYPE_PREFIX_LOOKUP) == 0) {
                        return LookupRuleType;
                    }
                } catch (IllegalArgumentException e ) {
                    //Ignored, gets thrown at end further below
                }
            }

            throw new IllegalArgumentException("Could not find a suitable rule type from operation " + pOp);
        }
    }
}
