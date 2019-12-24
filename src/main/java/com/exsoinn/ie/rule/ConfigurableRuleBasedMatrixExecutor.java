package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.ie.util.concurrent.SequentialTaskExecutor;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.Filter;
import com.exsoinn.util.epf.SearchPath;
import com.exsoinn.util.epf.TargetElements;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * TODO: Enhance so that it can execute any kind of rule, not just {@link QueryableRuleConfigurationData}
 * Created by QuijadaJ on 5/22/2017.
 */
class ConfigurableRuleBasedMatrixExecutor extends AbstractRule
        implements RuleExecutor<ConfigurableRuleBasedMatrixRule> {
    final static String GENERIC_CACHE_KEY = "genericConfigurableRuleBasedMatrixExecutor";

    ConfigurableRuleBasedMatrixExecutor(String pUniqueName) {
        super(pUniqueName);
    }


    @Override
    public  <T extends SearchPath, U extends Filter, V extends TargetElements>
    RuleExecutionResult applyBody(Context pContext,
                                  T pSearchPath,
                                  U pFilter,
                                  V pTargetElems,
                                  Map<String, String> pExtraParams) throws RuleException {

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, null);
        ConfigurableRuleBasedMatrixRule ruleMatrix =
                (ConfigurableRuleBasedMatrixRule) lookupRuleByName(pExtraParams.get(RuleConstants.RULE_NAME));

        // Execute each of the rules associated with this matrix, and save the results of each into a Map
        Map<String, Rule> rulesToExecute = ruleMatrix.getRulesToExecute();
        Set<Map.Entry<String, Rule>> ruleEnts = rulesToExecute.entrySet();
        /*
         * Map resultMap are the stores the output of each rule, and are what will be used as
         * input to calculate the matrix decision
         */
        Map<String, String> resultMap = new ConcurrentHashMap<>();
        List<RuleExecutionResult> resultObjs = new CopyOnWriteArrayList<>();
        List<Runnable> matrixRuleOps = new ArrayList<>();
        for (Map.Entry<String, Rule> ruleEnt : ruleEnts) {
            matrixRuleOps.add(() -> {
                try {
                    Rule ruleToExecute = lookupRuleByName(ruleEnt.getValue().name());
                    RuleExecutionResult ruleRes = ruleToExecute.apply(pContext, null, null, null, null);
                    resultObjs.add(ruleRes);
                    resultMap.put(ruleEnt.getKey(), ruleRes.evaluateResultAsString().toString());
                } catch (Throwable e) {
                    /**
                     * Because run() does not throw any checked exceptions, need to wrap into a
                     * <code>RuntimeException</code>.
                     */
                    throw new RuntimeException(e);
                }
            });
        }

        boolean interrupted = false;
        Throwable t;
        final ExecutorService execSvc;
        if (!AbstractRulesEngine.parallelizedRuleExecution()) {
            logDebug("Executing matrix rules ***sequentially***.");
            execSvc = SequentialTaskExecutor.SEQUENTIAL_TASK_EXECUTOR;
        } else {
            logDebug("Executing matrix rules in parallel.");
            execSvc = CommonUtils.autoSizedExecutor();
        }

        try {
            t = CommonUtils.runOperationsAsynchronouslyAndWaitForAllToComplete(execSvc,
                    matrixRuleOps.stream().map(e -> new FutureTask<Void>(e, null)).collect(Collectors.toList()),
                    AbstractRulesEngine.obtainTaskTimeoutSeconds());
        } catch (InterruptedException e) {
            /*
             * We can't modify this methods signature to declare InterruptedException, therefore catch it
             * and re-throw it. In the "finally" clause we restore interrupt status so that code that owns
             * this thread can handle appropriately as it pleases.
             */
            interrupted = true;
            throw new RuleException(e.toString(), e);
        } catch (TimeoutException e) {
            throw new RuleException(e.toString(), e);
        } finally {
            if (interrupted) {
                // Restore thread interrupted status so that thread owning code can handle appropriately
                Thread.currentThread().interrupt();
            }
        }

        if (null != t) {
            throw new RuleException("There was an error executing one or more rows of this rule-based matrix"
                    + ", rule name is " + ruleMatrix.name(), t);
        }

        // Using rule result Map built above (resultMap) build the input to make matrix decision,
        // and invoke the matrix rule to get the final decision
        MatrixDecisionInput mdi = MatrixDecisionInput.fromMap(resultMap);
        String matrixOutput = matrixDecision(ruleMatrix.name(), mdi, ruleMatrix.getDecisionFieldName());

        return new ConfigurableRuleBasedMatrixExecutorResult(false, info, matrixOutput, mdi,
                resultObjs, ruleMatrix, pContext, getOutputFieldMap());
    }



    static class ConfigurableRuleBasedMatrixExecutorResult extends TextOutputRuleExecutionResult {
        private final MatrixDecisionInput matrixDecisionInput;
        private final List<RuleExecutionResult> resultObjects;
        private final ConfigurableRuleBasedMatrixRule matrixComponent;

        ConfigurableRuleBasedMatrixExecutorResult(
                boolean pRes,
                Map<String, String> pInfo,
                String pOutput,
                MatrixDecisionInput pMatrixDecisionInput,
                List<RuleExecutionResult> pResObjs,
                ConfigurableRuleBasedMatrixRule pMatrixComponent,
                Context pInputCtx,
                Map<String, Rule> pOutFldMap) throws RuleException {
            super(pRes, pInfo, pOutput, pInputCtx, pOutFldMap);
            matrixDecisionInput = pMatrixDecisionInput;
            resultObjects = pResObjs;
            matrixComponent = pMatrixComponent;
        }


        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("\nMatrix Rules Executed Were:");
            sb.append(resultObjects.stream().map(RuleExecutionResult::toString).collect(Collectors.joining("\n")));

            sb.append("\n\nMatrix output is: ");
            // TODO: Parent AbstractRuleExecutionResult already invokes evaluateResult() and prints the result for us,
            // TODO: may be able to remove.
            try {
                sb.append(evaluateResultAsString().toString());
            } catch (RuleException e) {
                sb.append("Problem obtaining matrix result\n");
            }
            sb.append("\nMatrix decision input was: ");
            sb.append(matrixDecisionInput.toString());
            sb.append("\nMatrix Used:\n");
            sb.append(matrixComponent.context().stringRepresentation());
            sb.append("\nMatrix Component Name Is:\n");
            sb.append(matrixComponent.name());


            return sb.toString();
        }
    }
}
