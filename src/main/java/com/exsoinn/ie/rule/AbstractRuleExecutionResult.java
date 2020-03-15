package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.DnbBusinessObject;
import com.exsoinn.util.EscapeUtil;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.ContextFactory;
import com.exsoinn.util.epf.MutableContext;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.exsoinn.ie.rule.RuleConstants.CURRENT_CONTEXT;
import static com.exsoinn.ie.rule.RuleConstants.SUPPRESS_CONTEXT;

/**
 * Created by QuijadaJ on 4/19/2017.
 */
abstract class AbstractRuleExecutionResult implements RuleExecutionResult{
  private final Map<String, String> params;
  private final long startTimeInNanos;
  private final long endTimeInNanos;
  private final long totalExecutionTimeInNanos;
  private final Context context;
  private final Map<String, Rule> outputFieldMap;

  /**
   * The sole input parameter is consumed by {@link AbstractRuleExecutionResult#toString} to display
   * any information pertinent to the rule that produced this {@link RuleExecutionResult} object. It is
   * an arbitrary list of name/value pairs. The child {@link Rule} class can feel free to add anything here that
   * would be useful for instance to print to a log file in the context of troubleshooting an issue.
   *
   * @param pParams
   */
  AbstractRuleExecutionResult(Map<String, String> pParams, Context pInputCtx, Map<String, Rule> pOutFldMap) throws RuleException{
    params = pParams;
    if(!params.containsKey(RuleConstants.NANO_START_TIME)){
      throw new RuleException("Did not find the start time for this rule. This is required for performance metrics. " + "Rule information is " + params.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";")));
    }
    startTimeInNanos = Long.valueOf(params.get(RuleConstants.NANO_START_TIME));
    endTimeInNanos = System.nanoTime();
    totalExecutionTimeInNanos = endTimeInNanos - startTimeInNanos;
    pParams.put(RuleConstants.NANO_END_TIME, String.valueOf(endTimeInNanos));
    context = pInputCtx;
    outputFieldMap = pOutFldMap;
  }

  /**
   * Method {@link DnbBusinessObject#isEmpty()} does not make sense for this class, because this class
   * is not really a container of anything. Just by merely being instantiated it is "non-empty", therefore
   * this method will always return true.
   *
   * @return - Always return true, by virtue of being instantiated.
   */
  @Override
  public boolean isEmpty(){
    return false;
  }

  @Override
  public abstract boolean evaluateResult();

  @Override
  public Context evaluateResultAsContext(){
    throw new UnsupportedOperationException("This method  must be implemented.");
  }

  @Override
  public Context evaluateResultAsContext(boolean bSuppressNotApplicableFields){
    return evaluateResultAsContext();
  }

  /**
   * Parent {@code toString} method that all child class inherit, and can override/extend as needed.
   *
   * @return
   */
  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    sb.append("\nOutcome=");
    sb.append(evaluateResult());
    sb.append("\n\t");
    Map<String, String> filteredMap = params;

    /*
     * Added to avoid Travis CI error, fotr example https://travis-ci.com/github/joquijada/rule-api/jobs/298298869, which
     * fails build if log limit has been exceeded. It was observed that context printout was taking up a bulkd of the space in
     * the Travis build logs. By passing "-DsuppressContext" to the "mvn" command we can suppress printing the context.
     */
    if(null != System.getProperty(SUPPRESS_CONTEXT) && System.getProperty(SUPPRESS_CONTEXT).equalsIgnoreCase("Y")) {
      filteredMap = params.entrySet().stream().filter(e -> !e.getKey().equals(CURRENT_CONTEXT))
            .collect(Collectors.toMap(e -> e.getKey(), e -> null == e.getValue() ? "NULL value found" : e.getValue()));
    }

    if(null != filteredMap){
      sb.append(filteredMap.entrySet().parallelStream().map(Map.Entry::toString).collect(Collectors.joining("\n\t", "", "")));
    }

    sb.append("\nTotal execution time in seconds is " + totalExecutionTimeInSeconds());
    return sb.toString();
  }

  public Map<String, String> getParams(){
    return Collections.unmodifiableMap(params);
  }

  @Override
  public double totalExecutionTimeInSeconds(){
    return totalExecutionTimeInNanos / 1000000000D;
  }

  void populateOutputData(MutableContext pMutCtx) throws RuleException{
    if(null == outputFieldMap || outputFieldMap.isEmpty()){
      return;
    }

    Set<Map.Entry<String, Rule>> entries = outputFieldMap.entrySet();
    for(Map.Entry<String, Rule> e : entries){
      RuleExecutionResult res = e.getValue().apply(context, null, null, null, null);
      pMutCtx.addMember(e.getKey(), ContextFactory.obtainContext(res.evaluateResultAsString()));
    }
  }

  void addNameValuePairToContext(String pName, String pVal, MutableContext pMutCtx){
    if(CommonUtils.stringIsBlank(pVal)){
      pVal = "NULL";
    }else{
      /**
       * Replace characters that will cause context creation to fail with
       * an escape equivalent.
       */
      pVal = EscapeUtil.escapeSpecialCharacters(pVal);
    }
    pMutCtx.addMember(pName, ContextFactory.obtainContext(pVal));
  }

  @Override
  public String getOutput(){
    throw new UnsupportedOperationException("Method getOutput() must be implemented.");
  }
}
