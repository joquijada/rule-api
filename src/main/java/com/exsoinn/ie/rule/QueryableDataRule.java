package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.ie.util.concurrent.SequentialTaskExecutor;
import com.exsoinn.util.epf.*;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * Parent class to all classes that hold a data {@link Context}. This class encapsulates logic to
 * perform queries against the data stored inside this component. The data passed in during initialization is expected,
 * and must conform to this format (using JSON to represent it, but any format that can be parse by
 * {@link ContextFactory#obtainContext(Object)} will do):
 *
 * {"outer_element": [
 *   {"field1": "val",  "field2": "val", "field2": "val" },
 *   {"field1": "val",  "field2": "val", "field2": "val" },
 *   {...}
 *  ]}
 *
 *  Checkout the validate*() methods to see the various things that are enforced regarding the format of the config
 *  format above.
 *
 * The constructor will detect if a requested config has already been instantiated by a different rule object. If that's
 * the case, then instead of creating a new <code>Context</code>, and because {@code Context} objects are immutable,
 * the new rule object will point to existing one. Read
 * {@link QueryableDataRule#QueryableDataRule(Object, String)} for more details. This is called the Flyweight pattern by the way,
 *
 * Note: Because this is a package private class, to create visibility to certain methods, the class needs to be made
 *   to implement interfaces that define public methods that we want to make visible to calling code outside of the
 *   package. This does not mean clients can instantiate this class directly. For that they need to invoke one of
 *   the public static factory methods located in {@link AbstractRule}.
 *   An example of this is interface {@link IQueryableDataRule}, which defines method
 *   {@link IQueryableDataRule#context()}. We could have added that method in parent {@link Rule} interface, but then
 *   that would not be proper design, because {@link IQueryableDataRule#context()} does not apply to all rule
 *   types, therefore we opted to create a separate interface just to host that method, and any other methods in future
 *   which are relevant to {@link IQueryableDataRule} rules only..
 *
 * Created by QuijadaJ on 5/8/2017.
 */
@Immutable
@ThreadSafe
class QueryableDataRule extends AbstractRule implements IQueryableDataRule {
    static final String FOUND_ELEM_VAL_IS_REGEX = Context.FOUND_ELEM_VAL_IS_REGEX;
    static final String PARTIAL_REGEX_MATCH = Context.PARTIAL_REGEX_MATCH;
    private final Context ctx;
    private final List<DataSourceJoinInformation> dataSourceJoinInformation = new ArrayList<>();
    final static String ID_FLD_NAME = "id";
    private final List<String> fieldNames;
    private final String dataSourceName;
    private final boolean fieldValuesAreRegEx;


    /*
     * Constructors
     */
    QueryableDataRule(Object pObj, String pUniqueName) throws RuleException {
        this(pObj, pUniqueName, false, null);
    }

    QueryableDataRule(Object pObj, String pUniqueName, List<String> pOutFlds) throws RuleException {
        this(pObj, pUniqueName, false, pOutFlds);
    }

    /*
     * Share the internals by sharing the stored context: if a data source context with the same name *and* has already
     * been created, in a different rule object, and a {@code String.equals} of both returns true,
     * point to that one instead of creating a new instance here. This saves memory consumption in the JVM, and is also
     * a good Java practice as per EJ Bloch Item #15, Immutability. Because the context object is immutable, it's
     * OK for it to be shared among different QueryableDataRule objects. This is also called the Flyweight pattern.
     */
    QueryableDataRule(Object pObj, String pUniqueName, boolean pFldValsAreRegEx, List<String> pOutFlds) throws RuleException {
        super(pUniqueName, pOutFlds);
        try {
            Context tempCtx = ContextFactory.obtainContext(pObj);
            if (null == tempCtx) {
                throw new IllegalArgumentException("Could not parse the passed in data: " + pObj);
            }

            /**
             * If this rule object is trying to create a context which already was created by another rule object,
             * get a reference to it here instead of duplicating; saves JVM memory space.
             * TODO: Should we throw exception if string comparison is not same, even though using same name???
             * TODO: That check happens later in {@link AbstractRule#validateQueryableDataRuleComponents()},
             * TODO: Which enforces uniqueness of data source names, but question is, should we do it earlier inside this constructor???
             */
            Context foundCtx = lookupContextByDataSourceName(tempCtx.topLevelElementNames().get(0));
            ctx = (null != foundCtx && foundCtx.stringRepresentation().equals(tempCtx.stringRepresentation()))
                    ? foundCtx : ContextFactory.obtainContext(pObj);
            fieldNames = fieldNames();
            dataSourceName = dataSourceName();
            fieldValuesAreRegEx = pFldValsAreRegEx;
        } catch (IllegalArgumentException e) {
            throw new RuleException("Could not parse passed in data: " + pObj, e);
        }
    }



    @Override
    public Context context() {
        return ctx;
    }


    /**
     * When invoked will automatically detect if this data source joins with another. Then when querying this data source,
     * we can on the fly create a view of this data joined with the other data source's. This arose from repudiation
     * of duplicate data, which takes up more memory than necessary in the JVM. The auto detection of joins works
     * only if the developer configured the data source JSON's in the correct way.
     *
     * The rules and limitations governing this feature are:
     * 1) The field on this data source that will join with another *must* be a primitive or an array of primitives
     * 2) In order for a join connection to be made, the field from this data source to join on the target must
     *    match the name of the target's outer most element name. Example:
     *    This data source: {"this_data_src" : [{"field1": "val1", "fld2": "val2", "some_other_data_source": "id_val"}]}
     *    Target data source: {"some_other_data_source" : [{"field1": "val1", "fld2": "val2", "id": "id_val}]}
     *    In above example, notice the target data source top field name is "some_other_data_source", which happens to
     *    match field in source data source, namely "some_other_data_source"
     * 3) The value of the source data source from above will be matched against a field in the target data source
     *    which name *must* be "id".
     *
     * If any of the conditions above is not met, the system will not be able to automatically detect joins, and
     * this data source will *not* have a join with any other when queried.
     *
     * A data source can join with more than one target data source. Just follow the requirements above when configuring (I.e.
     * the name of the foreign key field *must* be exactly equal to the name of the outer most/top level element of the target
     * data source).
     *
     * Note: The reason this logic can't be calculated in object constructor is because we need to wait for all
     *   {@code IQueryableDataRule} to be fully initialized before we can start calculating join information.
     *
     */
    void initializeDataSourceJoinInformation()
            throws RuleException {
        logDebug("Initializing join info (if any) for '" + name() + "'...");
        Map.Entry<String, Context> dataSrcTopLvlElem = ctx.entrySet().iterator().next();
        List<Context> dataSrcRows = dataSrcTopLvlElem.getValue().asArray();
        // For every field in this context, iterate over the other QueryDataRule's, get startSearchPath for each, and if
        // it matches the field currently being iterated over, store a reference to the Rule object, and also this context's field name that
        // matched that other Rule object's startSearchPath field. This info gets stored in this object's
        // "dataSourceJoinInformation" instance member.
        Rule joinTargetDataSrc;
        String srcJoinFldFound;
        for (Map.Entry<String, Context> ent : dataSrcRows.get(0).entrySet()) {
            final String curMemberName = ent.getKey();
            Optional<Rule> ruleOpt = getRuleByNameCache().entrySet().parallelStream()
                    .filter(e -> !name().equals(e.getKey())) // Skip this rule object!
                    .filter(e -> e.getValue() instanceof IQueryableDataRule
                            && curMemberName.equals(((IQueryableDataRule) e.getValue()).context().startSearchPath().toString()))
                    .findAny().map(Map.Entry::getValue);

            if (ruleOpt.isPresent()) {
                joinTargetDataSrc = ruleOpt.get();
                /**
                 * The join field name is the field in the source data source component that is the
                 * foreign key to the target data source.
                 */
                srcJoinFldFound = curMemberName;
                DataSourceJoinInformation dsji =
                        new DataSourceJoinInformation((IQueryableDataRule) joinTargetDataSrc, srcJoinFldFound);
                dataSourceJoinInformation.add(dsji);
                logDebug("Detected that this data source joins with " + dsji.toString());
            }
        }



        if (dataSourceJoinInformation.isEmpty()) {
            logDebug("Done, no joins found for " + name());
        } else {
            logDebug("Done finding joins for " + name());
        }
    }


    /**
     * In order for a data source to be identified as a supplier, these conditions must be met:
     *
     *   1) No "id" field
     *   2) Two and only two fields
     *   3) Both fields are "foreign key" to ID fields of two other data sources
     *   4) Data source is *not* of type "QueryableRuleConfigurationData"
     *
     * @return - True if it is auto-detected that data source is a mapper, false otherwise
     */
    @Override
    public boolean dataSourceIsMapper() throws RuleException {
        if (this instanceof QueryableRuleConfigurationData) {
            return false;
        }

        List<String> fldNames = fieldNames();
        return !(fldNames.size() != 2 || fldNames.contains(ID_FLD_NAME) || dataSourceJoinInformation.size() != 2);
    }



    /**
     * Assembles rows of data based on the join information collected during startup by
     * {@link QueryableDataRule#initializeDataSourceJoinInformation()}.
     * This method will perform chained joins, meaning that if this data source joins with another data source that also
     * joins with another, then all those joins will be resolved first, beginning with the last data source in the
     * join chain, the last one in the chain, and ending with the main/first data source's join.
     * When an input record queries the join'able data source, filtering based on arbitrary list of name/value pairs is
     * supported; that's what parameter {@param pFilter} is for. However the filtering will be done on the main/primary data
     * source only, I.e. the one at the beginning of the "join" chain". This assumes there will be careful crafting
     * of joined data sources on the developers part. For example, the developer should ensure that there will not be
     * any circular dependencies, which should not be hard to do, if rule configuration world is relatively small
     * and not that complex.
     *
     * @param pFilter - To reduce the number of rows that need to be assembled. The filter values are filled in from
     *                attributes in the input record currently being evaluated.
     * @param pAssemblyRequestor - Used internally to break circular dependencies, in cases where two data sources mutually
     *                                  join, and one invokes assembleRuleConfigurationData() method of the other. When the other
     *                                  goes to assemble its joins, because of the mutual join involved, would ask the requestor
     *                                  to again assemble itself, resulting in infinite loop which eventually throws
     *                                  {@link StackOverflowError}. This {@code Map} is used to break the infinite loop.
     * @return
     * @throws RuleException
     */
    Context assembleRuleConfigurationData(Filter pFilter, Map<String, String> pAssemblyRequestor)
            throws RuleException, InterruptedException {
        return assembleRuleConfigurationData(ctx, pFilter, pAssemblyRequestor);
    }


    /**
     * Same as {@link QueryableRuleConfigurationData#assembleRuleConfigurationData(Filter, Map)}, but this signature
     * allows caller to pass their own <code>Context</code> to use to obtain rule set configurations, whereas the former
     * uses the member {@link QueryableRuleConfigurationData#context()} context.
     * @param pConfigCtx
     * @param pFilter
     * @param pAssemblyRequestor
     * @return
     * @throws RuleException
     * @throws InterruptedException
     */
    Context assembleRuleConfigurationData(Context pConfigCtx, Filter pFilter, Map<String, String> pAssemblyRequestor)
            throws RuleException, InterruptedException {
        /**
         * Algorithm:
         *
         * For each row of this data source,
         *   Iterate over each join info object, and for each delegate to thread the below work:
         *     Get the field which is the "foreign key" in this data source's current row
         *     Using foreign key, from that other data source, get the rows which have a field value that matches the
         *     foreign key from this data source
         *     Combine the fields from the other data source with this one's, omitting "id" from other data source
         */
        Context thisDataSrc;
        if (null == pAssemblyRequestor) {
            pAssemblyRequestor = new HashMap<>();
        }

        /**
         * The <code>Map</code> below is used to break circular dependencies when assembling dependency
         * contexts. See {@link QueryableDataRule#mergeForeignRows(DataSourceJoinInformation, Context, List, Map)}
         * for more info.
         */
        final Map<String, String> concurrentAssemblyRequestorMap = new ConcurrentHashMap<>(pAssemblyRequestor);

        StringBuilder assemMsg = new StringBuilder("Received assembly request, my rule name is '");
        assemMsg.append(name());
        if (null != pFilter) {
            logDebug(assemMsg.toString() + "', and was given filter " + pFilter.toString());
            Map<String, String> params = new HashMap<>(1);
            params.put(FOUND_ELEM_VAL_IS_REGEX, "1");
            SearchResult filteredCtx = pConfigCtx.findElement(pConfigCtx.startSearchPath(), pFilter, null, params);
            thisDataSrc = filteredCtx.get(pConfigCtx.startSearchPath().toString());
        } else {
            logDebug(assemMsg.toString() + ", NULL filter was given.");
            thisDataSrc = pConfigCtx.memberValue(pConfigCtx.startSearchPath().toString());
        }

        if (null == thisDataSrc) {
            return null;
        }

        List<Context> thisDataSrcRows = thisDataSrc.asArray();
        int totalRowsToMerge = thisDataSrcRows.size();
        logDebug("After applying filter " + (pFilter != null ? pFilter.toString() : "NULL") + ", " + name()
                + "'s resulting context has " + totalRowsToMerge + " rows to merge in, and the context is "
                + thisDataSrc.stringRepresentation());

        // If this source does not join with any other as per data captured when "initializeDataSourceJoinInformation()" was
        // called, return the results of applying filter above if pFilter was not NULL, else return the context as is
        if (dataSourceJoinInformation.isEmpty()) {
            logDebug("Data source component '" + name() + "' has nothing to join with"
                    + (null != pFilter ? ", returning filtered results." : ", returning context as is."));
            MutableContext thisCtx = ContextFactory.obtainMutableContext("{}");
            thisCtx.addMember(pConfigCtx.startSearchPath().toString(), thisDataSrc);
            return ContextFactory.obtainContext(thisCtx.stringRepresentation());
        }



        /**
         * Proceed to join each row in this data source, with rows of other data sources. We partition the rows if partition
         * threshold has been met (see {@link QueryableDataRule#partitionContextRows(List)} for details), and let X
         * number of threads handle each partition in parallel
         * The merged rows are collected in a cumulative {@link MutableContext} object. To make things thread-safe,
         * and so that it gets safely published once modified by other threads, we wrap this {@link MutableContext}
         * into an {@link AtomicReference}. This enables safe publishing of joined rows from the child threads that
         * did the joining, back to the parent thread.
         */
        final AtomicReference<MutableContext> mergedCtxRowsAtomRef
                = new AtomicReference<>(ContextFactory.obtainMutableContext("[]"));
        List<Runnable> rowAssemOps = new ArrayList<>();
        /**
         * TODO: Make partition size configurable. Below is using configured thread pool size because
         *   it was coded before implementation of auto sized thread pool, when the thread pool size setting
         *   was being treated as global per thread, instead of global per app, which was resulting in out-of-memory crash
         *   due to unwieldy number of threads getting created (because every thread was acting in its own silo
         *   when it came to setting a limit on how many threads it could create)
         */
        int threadPoolsize =
                Integer.valueOf(AbstractRulesEngine.fetchConfigurationPropertyValue(RuleConstants.CONFIG_PROP_THREAD_POOL_SIZE));
        int partSize = totalRowsToMerge/threadPoolsize;
        final Map<Integer, List<Context>> ctxRowParts = partitionContextRows(thisDataSrcRows, partSize);
        for (Map.Entry<Integer, List<Context>> ent : ctxRowParts.entrySet()) {
            List<Context> partRows = ent.getValue();
            rowAssemOps.add(() -> {
                try {
                    assembleContextRows(partRows, mergedCtxRowsAtomRef, concurrentAssemblyRequestorMap);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        }


        /**
         * Use an executor to perform joins for each row partition concurrently, except if parallelization
         * is turned of, or there's less than 2 row partitions that were created above. In which case,
         * perform the operation sequentially using. That's what the {@link SequentialTaskExecutor} is for.
         */
        Throwable t;
        final ExecutorService es;
        if (!AbstractRulesEngine.parallelizedRuleExecution() || rowAssemOps.size() < 2) {
            logDebug("Executing each row join ***sequentially***.");
            es = SequentialTaskExecutor.SEQUENTIAL_TASK_EXECUTOR;
        } else {
            logDebug("Executing each row join in ***parallel***.");
            es = CommonUtils.autoSizedExecutor();
        }

        try {
            t = CommonUtils.runOperationsAsynchronouslyAndWaitForAllToComplete(es,
                    rowAssemOps.stream().map(e -> new FutureTask<Void>(e, null)).collect(Collectors.toList()),
                    AbstractRulesEngine.obtainTaskTimeoutSeconds());
        } catch (TimeoutException e) {
            throw new RuleException(e.toString(), e);
        }

        if (null != t) {
            throw new RuleException("There was a problem assembling one or more of the rows partition, component name is "
                    + this.name(), t.getCause());
        }


        /**
         * All context row partitions assembled (in "mergedCtxRowsAtomRef"), now combine
         * them into final Context object. Because we have used an {@link AtomicReference}
         * object to hold our {@link MutableContext}, changes will be fully visible to this thread
         * (safe publication, Goetz06 Java Concurrency in Practice, 3.5, page 49), if threads were used
         * to put populate the {@link MutableContext}.
         */
        MutableContext mergedCtx = ContextFactory.obtainMutableContext("{}");
        mergedCtx.addMember(pConfigCtx.startSearchPath().toString(), mergedCtxRowsAtomRef.get());


        /**
         * VERY IMPORTANT: So that the invariants of our input record Context object remain true, turn the mutable context
         *   back into an immutable one before sharing with the world.
         */
        Context immutableCtx = ContextFactory.obtainContext(mergedCtx.stringRepresentation());
        logDebug("Done with assembly request, my rule name is '" + name()
                + ", final context built is " + immutableCtx.stringRepresentation());

        return immutableCtx;
    }


    /**
     * This method will iterate over each row passed in {@param pCtxRows}, and for each field that joins to another
     * source, merge the joined row fields into each of the rows passed in {@param pCtxRows}.
     * @param pCtxRows - The context rows into which foreign source fields will be merged, if there are any joins with
     *                   other data sources
     * @param pMergedCtxRowsAtomRef - Where the resulting merged rows will be put. Internally this method
     *                              synchronizes on the {@link MutableContext} object held by this
     *                              {@link AtomicReference}, because it is mutable, and to make things thread-safe
     *                              thread-safe.
     * @param pAssemblyRequestor - A {@code Map} used to break circular dependencies, so that the context
     *                           currently assembling itself does not ask the requestor of its assembly to
     *                           assemble itself, else we end up with an infinite cycle.
     * @throws InterruptedException
     * @throws RuleException
     */
    void assembleContextRows(List<Context> pCtxRows,
                             AtomicReference<MutableContext> pMergedCtxRowsAtomRef,
                             Map<String, String> pAssemblyRequestor) throws InterruptedException, RuleException {
        int totalRowsToMerge = pCtxRows.size();

        logDebug("This worker received " + totalRowsToMerge + " context rows to assemble on behalf of component name "
                + this.name());
        int cnt = 1;
        for (Context thisDataSrcRow : pCtxRows) {
            /**
             * Initialize a context that has this row's members. It will be used farther down to merge with
             * foreign data source, if there are any joins. We make it an {@link AtomicReference} for the
             * multi-thread case so that the object is safely published after other threads are done modifying it.
             */
            MutableContext thisRowMembers = ContextFactory.obtainMutableContext("{}");
            final AtomicReference<List<MutableContext>> addedRowsAtomRef = new AtomicReference<>(new ArrayList<>());
            addedRowsAtomRef.get().add(thisRowMembers);
            thisDataSrcRow.entrySet().stream().forEach((e) -> thisRowMembers.addMember(e.getKey(), e.getValue()));


            List<Runnable> joinOps = new ArrayList<>();
            /**
             * Iterate over each join object of this data source, and for each find the rows to merge with this data source's
             * rows, combining the fields into one single row.
             */
            for (DataSourceJoinInformation dsji : dataSourceJoinInformation) {
                /*
                 * To break circular dependencies, check if this data source joins to a data source which has requested
                 * this data source to assemble itself, in which case skip it, else we'll
                 * find ourselves in a circular dependency situation which gives StackOverFlow
                 */
                if (pAssemblyRequestor.containsKey(dsji.getTargetDataSource().name())) {
                    logDebug("Will not ask the requestor '" + dsji.getTargetDataSource().name()
                            + "' of my assembly to assemble itself, for it would result in an infinite loop, skipping this join: "
                            + dsji.toString());
                    continue;
                }
                /**
                 * For each join, build a Runnable so it can be executed asynchronously further below
                 * using common utility method that runs things asynchronously. In keeping with the contract so that
                 * CommonUtils.runOperationsAsynchronously() can deliver what it guarantees, I must catch and wrap any
                 * exception into a RuntimeException. Then "CommonUtils.runOperationsAsynchronously()" will examine
                 * the cause of my exception and return it to me. Also it will take care of appropriately
                 * handling InterruptedException, if any. See "CommonUtils.runOperationsAsynchronously()" javadoc for more
                 * details.
                 */
                joinOps.add(() -> {
                    try {
                        mergeForeignRows(dsji, thisDataSrcRow, addedRowsAtomRef, pAssemblyRequestor);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            if (!joinOps.isEmpty()) {
                /**
                 * For now use single thread executor to run field join operations sequentially. In future and if it helps
                 * make things run faster, we reserve right to execute in parallel each field join, in which case must
                 * wait for all joins to finish successfully; method call below would take care of that, and if there was
                 * an error will return non-null Throwable.
                 */
                Throwable t;
                final ExecutorService es;
                if (!AbstractRulesEngine.parallelizedRuleExecution()) {
                    logDebug("Executing join operations ***sequentially***.");
                    es = SequentialTaskExecutor.SEQUENTIAL_TASK_EXECUTOR;
                } else {
                    logDebug("Executing join operations in ***parallel***.");
                    es = CommonUtils.autoSizedExecutor();
                }
                try {
                    t = CommonUtils.runOperationsAsynchronouslyAndWaitForAllToComplete(es,
                            joinOps.stream().map(e -> new FutureTask<Void>(e, null)).collect(Collectors.toList()),
                            AbstractRulesEngine.obtainTaskTimeoutSeconds());
                } catch (TimeoutException e) {
                    throw new RuleException(e.toString(), e);
                }

                if (null != t) {
                    throw new RuleException("There was an error merging with foreign source(s)", t.getCause());
                }
            }

            /**
             * VERY IMPORTANT: Multiple threads will be attempting to write to pMergedCtxRowsAtomRef.get(), therefore
             *   must synchronize on mutable pMergedCtxRowsAtomRef.get(), both for mutual exclusion atomicity,
             *   and to guarantee other threads see the state visible to a thread right before it release the lock
             *   on pMergedCtxRows. See Goetz06 3.1, Java Concurrency in Practice, pages 33 - 39.
             */
            synchronized(pMergedCtxRowsAtomRef.get()) {
                for (MutableContext mc : addedRowsAtomRef.get()) {
                    pMergedCtxRowsAtomRef.get().addEntryToArray(mc);
                }
            }
            logDebug("Done ('" + name() + "'), added row(s) "
                    + addedRowsAtomRef.get().stream().map(Context::stringRepresentation).collect(Collectors.joining(","))
                    + ", move on to next row. " + cnt + " row(s) completed out of a total of " + totalRowsToMerge);
            ++cnt;
        }

        logDebug("Done assembling " + totalRowsToMerge + " context rows on behalf of component name " + this.name());
    }


    /**
     * Thread-safe method to merge foreign rows together with this source's row
     * @param pJoinInfo - The {@link DataSourceJoinInformation} object that has information of the field in
     *                  {@param pThisRow} that should be used as the "foreign key" to join with
     *                  {@param pJoinInfo}'s {@link DataSourceJoinInformation#getTargetDataSource()}
     * @param pThisRow - The current row of this component that is being merged with the other data source
     * @param pMergedRowsAtomRef - An {@link AtomicReference} that holds the cumulative list of
     *                           {@link MutableContext} objects that result from merging foreign rows into this
     *                           components rows.
     * @param pAssemblyRequestor
     * @throws RuleException
     * @throws InterruptedException
     */
    void mergeForeignRows(DataSourceJoinInformation pJoinInfo,
                          Context pThisRow,
                          AtomicReference<List<MutableContext>> pMergedRowsAtomRef,
                          Map<String, String> pAssemblyRequestor) throws RuleException, InterruptedException {
        Context joinFldValCtx = pThisRow.memberValue(pJoinInfo.getSourceJoinFieldName());
        String joinFldValStr = joinFldValCtx.stringRepresentation();

        /**
         * Instruct that target data source we're trying to join to do its own assembly first,
         * and for efficiency's sake use the "foreign key" value found above, so that it assembles only the rows that this
         * data source's row is interested in as per the foreign key value in this data source's current row
         */
        StringBuilder sb = new StringBuilder();
        sb.append(pJoinInfo.getTargetJoinFieldName());
        sb.append("=");
        sb.append(joinFldValStr);
        QueryableDataRule otherDataSrcRuleComponent = (QueryableDataRule) pJoinInfo.getTargetDataSource();
        Filter otherDataSrcFilter = Filter.valueOf(sb.toString());
        logDebug("Begin join with component '" + otherDataSrcRuleComponent.name()
                + "', will tell it to do its own join first, using filter " + otherDataSrcFilter.toString());

        /**
         * Read description of pDataSrcAssemblyRequested parameter in
         * {@link QueryableDataRule#assembleRuleConfigurationData(Filter, Map)}'s documentation to learn why
         * we do this. Before telling the other component to assemble itself, add ourselves to this Map so that
         * downstream component (see "continue" check in method
         * {@link QueryableDataRule#assembleRuleConfigurationData(Filter, Map)}, inside the join object loop)
         * won't try to join back with us, which causes circular dependency resulting in infinite loop.
         */
        pAssemblyRequestor.put(name(), "1");
        String otherDsName = otherDataSrcRuleComponent.dataSourceName();


        /**
         * See if for the given {@code Filter}, a {@code Context} was already loaded into memory. For now
         * this applies to list entry data sources ({@link AbstractRule#dataSourceIsEntryContainer(Rule)} is
         * <code>true</code>), because those can grow quite large and we needed to optimize.
         */
        List<Context> otherDataSrcRows = null;
        Map<String, Context> dsFromEntryCache = listEntryContextLookupMap.get(otherDsName);
        if (null != dsFromEntryCache) {
            Context entryCtx = dsFromEntryCache.get(otherDataSrcFilter.toString());
            if (null != entryCtx) {
                otherDataSrcRows = new ArrayList<>();
                otherDataSrcRows.add(entryCtx);
                logDebug("Found list entry in map for component " + this.name()
                        + ". Entry found is " + entryCtx.stringRepresentation() + ", and filter provided was "
                        + otherDataSrcFilter.toString());
            }
        }


        if (null == otherDataSrcRows) {
            logDebug("Did *NOT* find list entry in map for component " + this.name()
                    + "(ignore if this component is not trying to join with a list entry data source). Filter provided was "
                    + otherDataSrcFilter.toString() + ". Therefore will request assembly of "
                    + otherDataSrcRuleComponent.name());
            Context otherDataSrcCtx =
                    otherDataSrcRuleComponent.assembleRuleConfigurationData(otherDataSrcFilter, pAssemblyRequestor);

            /**
             * Now that other component finished with its assembly, remove ourselves so that component upstream
             * that requested our assembly won't skip joining with us again on the next row iteration
             */
            pAssemblyRequestor.remove(name());

            /**
             * Throw exception if join failed to yield any rows. This might alert the configs author in a timely manner of
             * any configuration issues.
             */
            if (null == otherDataSrcCtx) {
                throw new RuleException("No rows found during assembly when trying to join. The target component is '"
                        + otherDataSrcRuleComponent.name() + "'. The filter used to try to join with that component was '"
                        + otherDataSrcFilter.toString() + "'. The row of this component (" + this.name() + ") "
                        + "that would have merged into is " + pThisRow.stringRepresentation() + "\nThe other components "
                        + " context is " + otherDataSrcRuleComponent.context().stringRepresentation());
            }

            Context otherDataSrc = otherDataSrcCtx.memberValue(otherDataSrcCtx.startSearchPath().toString());

            String doneMsg = "Done, other data source's join assembly complete (" + otherDataSrcRuleComponent.name()
                    + ")";
            if (null == otherDataSrc || otherDataSrc.asArray().isEmpty()) {
                logDebug(doneMsg + ", found no rows, nothing to join with, therefore will return");
                return;
            } else {
                logDebug(doneMsg);
            }

            logDebug("Beginning to do our own join (my name is " + this.name() + ") with the other component which just "
                    + "assembled itself ('" + otherDataSrcRuleComponent.name()
                    + "'); the context obtained from that data source is " + otherDataSrcCtx.stringRepresentation());


            /**
             * As an extra check throw exception if the target data source field name we're joining with is "id" and
             * more than one row is returned. The exceptions to this rule are:
             *   1) When the other data source itself joins with a data mapper, in which case multiple rows for same ID
             *      are expected/allowed.
             *   2) The other data source could have specified a list of ID's to join from the target data source it
             *      joins with, in which case multiple rows can be expected during assembly.
             *      TODO: Is it correct that below grabs from list the first join info object of other data source? The below hasn't
             *      TODO:   failed because it applies to list/entry data sources (the "other" data source), which usually join with
             *      TODO:   only one data source, namely a data mapper. Need to keep an eye on this in future in case we can
             *      TODO:   have scenario where a data source can join with both a data mapper and a non-data mapper. To avoid
             *      TODO:   future issues, better to write logic that tells if any of the other data source's join is a data mapper.
             *      TODO:   Alternatively, can add init check to ensure that sources that join with data mappers cannot join with
             *      TODO:   another source (I.e. one and only one join allowed if you want/need to join with a data mapper).
             */
            QueryableDataRule otherDataSrcJoinedDataSrc = !otherDataSrcRuleComponent.getDataSourceJoinInformation().isEmpty() ?
                    (QueryableDataRule) otherDataSrcRuleComponent.getDataSourceJoinInformation().get(0).getTargetDataSource()
                    : null;

            otherDataSrcRows = otherDataSrc.asArray();
            if (Context.transformArgumentToListObject(joinFldValStr) == null
                    && !dataSourceJoinCanYieldMultipleRows(otherDataSrcRuleComponent, otherDataSrcCtx)
                    && pJoinInfo.getTargetJoinFieldName().equals(ID_FLD_NAME)
                    && otherDataSrcRows.size() > 1 &&
                    (null == otherDataSrcJoinedDataSrc || !otherDataSrcJoinedDataSrc.dataSourceIsMapper())) {
                throw new RuleException("Got more than one row for the same ID filter " + sb.toString()
                        + ". Check that data format of target data source is correct: "
                        + otherDataSrc.stringRepresentation() + "\nMy component name is " + this.name()
                        + ", the target component I'm trying to join with is " + otherDataSrcRuleComponent.name());
            }
        }


        /**
         * Ok, we got one or more rows from the other data source. Merge this
         * and the other rows' members by repeating this data source's fields times
         * the number of rows found in the other data source
         * At all times exclude the other source's ID field.
         * Also have to synchronize, because it has to be atomic the act of reading current contents of
         * pMergedRowsAtomRef.get(), iterating over the found foreign source's rows, and merging those back into the cumulative
         * pMergedRowsAtomRef.get() list - remember that multiple threads are actively trying to merge into the same
         * resource, namely pMergedRowsAtomRef.get()
         * Which is the reason that the mutable context has been wrapped in an {@link AtomicReference} object:
         * We want to safely publish the results of all threads back to parent code, and this
         * can only be accomplished by a using a structure that is synchronized, to guarantee the "happens before".
         * See Java Concurrency in Practice, Goetz 2005, section 3.5.3, page #52 towards the middle.
         */
        synchronized (pMergedRowsAtomRef.get()) {
            List<MutableContext> tempRows = new ArrayList<>();
            for (Context otherDataSrcRow : otherDataSrcRows) {
                for (Context r : pMergedRowsAtomRef.get()) {
                    MutableContext mergedRow = ContextFactory.obtainMutableContext(r.stringRepresentation());
                    otherDataSrcRow.entrySet().stream()
                            .filter(e -> !ID_FLD_NAME.equals(e.getKey()))
                            .forEach(e -> mergedRow.addMember(e.getKey(), e.getValue()));
                    tempRows.add(mergedRow);
                }
            }
            pMergedRowsAtomRef.get().clear();
            pMergedRowsAtomRef.get().addAll(tempRows);
            logDebug("After joining with '" + otherDataSrcRuleComponent.name() + "', resulting context is "
                    + pMergedRowsAtomRef.get().stream().map(e -> e.stringRepresentation()).collect(Collectors.joining(",")));
        }
    }


    private Map<Integer, List<Context>> partitionContextRows(List<Context> pCtxRows, final int pPartSize) {
        final int threshold = 1000;
        final Map<Integer, List<Context>> partitions = new LinkedHashMap<>();
        final int numRows = pCtxRows.size();
        /*
         * We have not met partition threshold, nothing to partition, return
         * a List but with just the single partition in it.
         */
        if (null == pCtxRows || numRows < threshold) {
            partitions.put(Integer.valueOf("1"), pCtxRows);
            logDebug("Component name " + name() + " with a context rows size of " + numRows
                    + " did not meet partition threshold of " + threshold);
            return partitions;
        }


        /*
         * We have met partition threshold, go ahead and partition
         */
        logDebug("For component " + this.name() + " with a size of " + numRows + ", partition threshold of "
                + threshold + " has been met. The context row partition size will be " + pPartSize);

        List<Context> partRows = new ArrayList<>();
        int partNum = 1;
        for (int i = 0; i < numRows; i++) {
            if (i % pPartSize == 0) {
                /*
                 * Current partition filled up, time to initialize a new one
                 * and store it in the final partitions Map
                 */
                partRows = new ArrayList<>();
                partitions.put(Integer.valueOf(partNum), partRows);
                ++partNum;
            }

            partRows.add(pCtxRows.get(i));
        }

        logDebug("For component name " + name() + ", context rows total partitions created is " + partitions.size());
        return partitions;
    }

    /**
     * Returns <code>true</code> if the passed in {@link QueryableDataRule} can be expected to yield multiple rows
     * once it joins with the passed in {@param pDataSrcCtx}. This is done by examining the contents of the passed in
     * <code>pDataSrcCtx</code>, which is expected to be the <code>Context</code> built from joining
     * <code>pDataSrc</code> with another one and seeing if the join field of <code>pDataSrc</code> is a list
     * of ID's.
     *
     * @param pDataSrc - The data source to check if joining with another data source can yield more than one row.
     * @param pDataSrcCtx - The assembled <code>Context</code> produced by executing one of the joins of {@param pDataSrc}
     * @return - True if the check passes, false otherwise.
     */
    boolean dataSourceJoinCanYieldMultipleRows(QueryableDataRule pDataSrc, Context pDataSrcCtx) {
        String otherDataSrcName = pDataSrcCtx.startSearchPath().toString();
        for (DataSourceJoinInformation dsji : pDataSrc.getDataSourceJoinInformation()) {
            /**
             * Remember, the "dsji.getSourceJoinFieldName()" gives the name of a field in {@param pDataSrc} that bears the name
             * of the foreign data source it joins with.
             */
            final String targetJoinFldName = dsji.getSourceJoinFieldName();

            /*
             * Ensure we're checking the join info that corresponds to the target data source that produced
             * the passed in assembled Context
             */
            if (!otherDataSrcName.equals(targetJoinFldName)) {
                continue;
            }
            List<Context> rows = pDataSrcCtx.memberValue(otherDataSrcName).asArray();

            /**
             * Below is saying that if this data source (pDataSrc) had a comma-delimited list as the
             * foreign (targetJoinFldName) field value, then it means it would grab more than one row from the other
             * (aka foreign) data source.
             */
            return rows.parallelStream().
                    map(e -> Context.transformArgumentToListObject(e.memberValue(targetJoinFldName).stringRepresentation())).
                    filter(e -> null != e).findAny().isPresent();
        }
        return false;
    }



    /**
     * The values to use to query the data that this component holds can come from an input record itself. Placeholders in the
     * query filter will get replaced with data from the search results of the input record {@code Context} passed into the
     * {@link QueryableDataRule#apply(Context, SearchPath, Filter, TargetElements, Map)} method. For example, if the input record
     * (aka <code>Context</code>) search results contain a field/value FIELD_NAME=FIELD_VALUE, then the query filter
     * passed in {@param pExtraParams} in key {@link RuleConstants#QUERY_FILTER} can contain a value like
     * <code>QRY_FILTER_KEY=<FIELD_NAME></code>. This tells the code to replace <FIELD_NAME/> with the
     * <code>FIELD_NAME</code> found in <code>Context</code>.
     *
     * Note/Clarification: Confusingly enough, and because this method can query two separate {@link Context}'s,
     * the pContext is searched using the other arguments passed in the method, and the {@link Context} that this
     * component holds is queried using the arguments passed in pExtraParams:
     *   RuleConstants.QUERY_SEARCH_PATH
     *   RuleConstants.QUERY_RETURN_ELEMENTS
     *   RuleConstants.QUERY_FILTER
     * TODO: Re-visit later to see if this confusion can be somehow reconciled/cleaned up
     *
     *
     * @param pContext
     * @param pSearchPath
     * @param pFilter
     * @param pTargetElems
     * @param pExtraParams
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

        /*
         * From the data record, get the value we want to use to query the data contained in
         * this component. If pContext is NULL, the search parameters can be found in pExtraParams
         */
        SearchResult srcData = null;
        if (null != pContext) {
            srcData = pContext.findElement(pSearchPath, pFilter, pTargetElems, pExtraParams);
        }


        /*
         * Now using the value acquired above and in conjunction with config arguments passed by caller,
         * "query" the data that this rule block holds.
         */
        TargetElements te = TargetElements.valueOf(pExtraParams.get(RuleConstants.QUERY_RETURN_ELEMENTS));
        Filter filter;
        if (null != srcData) {
            /*
             * We're using input record as source of values to dynamically populate the
             * "filter clause" to query the data hosted by this object...
             */
            filter = parseFilter(pExtraParams.get(RuleConstants.QUERY_FILTER), srcData);
        } else {
            /*
             * ...otherwise we're using a constant, passed in filter
             */
            filter = Filter.valueOf(pExtraParams.get(RuleConstants.QUERY_FILTER));
        }
        SearchResult result = ctx.findElement(ctx.startSearchPath(), filter, te, pExtraParams);

        Map<String, String> info = populateCommonResultProperties(
                pContext, pSearchPath, pFilter, pTargetElems, pExtraParams, result);
        info.putAll(pExtraParams);

        return new QueryableDataRuleExecutionResult(!result.isEmpty(), info, result, pContext, getOutputFieldMap());
    }


    /**
     * Gives a list of the fields names contained in the underlying data object.
     * TODO: Revisit, this is doing redundant TopLevelElementValueIsNotArrayException check which might interfere
     * TODO: with JUnit testing
     * @return
     * @throws TopLevelElementValueIsNotArrayException
     */
    List<String> fieldNames() throws TopLevelElementValueIsNotArrayException {
        if (null != fieldNames) {
            return fieldNames;
        }
        List<String> topElemNames = context().topLevelElementNames();
        SearchResult sr = context().findElement(SearchPath.valueOf(topElemNames.get(0)), null, null, null);
        Context rows = sr.get(topElemNames.get(0));
        if (!rows.isArray()) {
            throw new TopLevelElementValueIsNotArrayException(context());
        }

        Context oneRow = rows.entryFromArray(0);
        return oneRow.entrySet().parallelStream().map(Map.Entry::getKey).collect(Collectors.toList());
    }


    /*
     * Given a query filter and source data, it will substitute placeholders (enclosed in angle brackets) with the matching
     * field name value found in pSrcData.
     */
    private Filter parseFilter(String pFilterStr, SearchResult pSrcData) {
        if (CommonUtils.stringIsEmpty(pFilterStr)) {
            return null;
        }

        Filter f = Filter.valueOf(pFilterStr);
        Map<String, String> updatedFilter = new HashMap<>(f);
        f.entrySet().parallelStream()
                .forEach(e -> updatedFilter.put(e.getKey(), pSrcData.get(removeOuterAngleBrackets(e.getValue())).stringRepresentation()));


        return Filter.fromMap(updatedFilter);
    }

    private String removeOuterAngleBrackets(String pInStr) {
        int begin = 0;
        int end = pInStr.length();
        if (pInStr.indexOf("<") == 0) {
            begin = 1;
        }

        if (pInStr.indexOf(">") == end - 1) {
            end = end - 1;
        }

        return pInStr.substring(begin, end);
    }


    static class QueryableDataRuleExecutionResult extends BooleanRuleExecutionResult
            implements SearchResultRuleExecutionResult {
        private final Map<String, Context> searchResult;

        QueryableDataRuleExecutionResult(
                boolean pVal,
                Map<String, String> pParams,
                Map<String, Context> pSearchResult,
                Context pInputCtx,
                Map<String, Rule> pOutFldMap)
                throws RuleException {
            super(pVal, pParams, pInputCtx, pOutFldMap);
            searchResult = pSearchResult;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());

           /*
            * TODO: JMQ: Removed temporarily Include search results if any were provided when this object was created
            */
            /*if (null != searchResult) {
                sb.append("\n\tSearch Results\n\t\t");
                sb.append(searchResult.entrySet().parallelStream().map(Map.Entry::toString)
                        .collect(Collectors.joining("\n\t\t", "", "")));
            }*/

            return sb.toString();
        }

        @Override
        public Map<String, Context> getSearchResult() {
            return new HashMap<>(searchResult);
        }
    }



    @Override
    public List<DataSourceJoinInformation> getDataSourceJoinInformation() {
        return dataSourceJoinInformation;
    }


    /**
     * Validations common to this and child classes.
     *
     * 1) Only on top level element defined in the data structure.
     * 2) The value of the sole top element must be an array.
     * 3) Row level validations - see {@link QueryableDataRule#validateRows()}
     * @throws RuleException
     */
    void validateQueryableDataFormat() throws RuleException {
        validateOnlyOneTopElement();
        validateTopElementValueIsArray();
        validateRows();
    }


    void performPostJoinInitializationValidations() throws RuleException {
        validateDataMapperJoinExclusivity();
    }

    /**
     *
     * @throws InvalidQueryableDataFormatException
     */
    void validateOnlyOneTopElement() throws InvalidQueryableDataFormatException {
        if (context().topLevelElementNames().size() > 1) {
            throw new InvalidQueryableDataFormatException("Can have at most only one outer most element, check configuration"
                    + " and try again: " + context().stringRepresentation());
        }
    }


    /**
     *
     * @throws InvalidQueryableDataFormatException
     */
    void validateTopElementValueIsArray() throws TopLevelElementValueIsNotArrayException {
        if (!context().memberValue(context().startSearchPath().toString()).isArray()) {
            throw new TopLevelElementValueIsNotArrayException(context());
        }
    }


    /**
     * Child classes can override this method to do nothing. This is what this method validates,
     * for every row:
     *
     *   1) A row must be a a complex object
     *   2) All field values inside each row must be type primitive or array only.
     *   3) If one row contains "id" field, then rest of rows must have it
     *   4) All "id" values must be unique
     *   5) All rows must contain the same set of fields - The check is done by taking the fields of the first row, and making
     *     sure all rows have the exact same fields.
     *
     * @throws InvalidQueryableDataFormatException
     * @throws DuplicateRowIdException
     * @throws MissingIdFieldException
     * @throws RowIsNotComplexException
     * @throws NotAllRowFieldValuesArePrimitiveOrArray
     */
    void validateRows()
            throws InvalidQueryableDataFormatException,
            DuplicateRowIdException, MissingIdFieldException, RowIsNotComplexException,
            NotAllRowFieldValuesArePrimitiveOrArray, NotAllRowsHaveSameFieldsException {
        Map<String, Boolean> seenIds = new HashMap<>();
        int curRowCnt = 1;
        List<Context> rows = context().memberValue(context().startSearchPath().toString()).asArray();
        /**
         * To validate that all rows contain same set of fields, arbitrarily store the first row's
         * fields, and then compare the rest of the rows against it and throw error if fields of one
         * row do not exactly match the expected ones.
         */
        String expectedRowFlds = rows.get(0).entrySet().stream().map(e -> e.getKey()).sorted().collect(Collectors.joining(","));
        for (Context row : rows) {
            String thisRowFlds = row.entrySet().stream().map(e -> e.getKey()).sorted().collect(Collectors.joining(","));
            if (!thisRowFlds.equals(expectedRowFlds)) {
                throw new NotAllRowsHaveSameFieldsException(this, expectedRowFlds, thisRowFlds, row, context());
            }
            validateRowIsComplexObject(row);
            validateAllRowFieldValuesArePrimitiveOrArray(row);
            validateIdFieldRequirements(row, curRowCnt, seenIds);
            curRowCnt++;
        }
    }

    /**
     * A row must be a complex object. E.g. if the data format was JSON, each row would have to be enclosed in "{}".
     * @param pRow
     * @throws RowIsNotComplexException
     */
    void validateRowIsComplexObject(Context pRow) throws RowIsNotComplexException {
        if (!pRow.isRecursible()) {
            throw new RowIsNotComplexException(pRow);
        }
    }

    /**
     * Checks that all field values in all rows are primitive or array only.
     * TODO: Add check to ensure that array's contain primitives only.
     * @param pRow
     * @throws NotAllRowFieldValuesArePrimitiveOrArray
     */
    void validateAllRowFieldValuesArePrimitiveOrArray(Context pRow) throws NotAllRowFieldValuesArePrimitiveOrArray {
        Set<Map.Entry<String, Context>> rowFlds = pRow.entrySet();
        Optional<Map.Entry<String, Context>> found =
                rowFlds.stream().filter(e -> !e.getValue().isPrimitive() && !e.getValue().isArray()).findFirst();
        if (found.isPresent()) {
            throw new NotAllRowFieldValuesArePrimitiveOrArray(found.get().getValue());
        }
    }


    /**
     * Validates that if one row contains field named "id", then all rows have it as well, and that each "id"
     * field value is indeed unique.
     *
     * @param pRow
     * @param pCurRowCnt
     * @param pSeenIds
     * @throws DuplicateRowIdException
     * @throws MissingIdFieldException
     */
    void validateIdFieldRequirements(Context pRow, int pCurRowCnt, Map<String, Boolean> pSeenIds)
            throws DuplicateRowIdException, MissingIdFieldException {
        if (pRow.containsElement(ID_FLD_NAME)) {
            String idVal = pRow.memberValue(ID_FLD_NAME).stringRepresentation();
            if (pSeenIds.containsKey(idVal)) {
                throw new DuplicateRowIdException(this, pRow);
            }
            pSeenIds.put(idVal, Boolean.TRUE);
        }

        if (!pSeenIds.isEmpty() && pCurRowCnt != pSeenIds.size()) {
            throw new MissingIdFieldException(pRow);
        }
    }


    /**
     * Enforces that if this data sources joins with a data mapper, then it cannot have other joins, period.
     * @throws RuleException
     */
    void validateDataMapperJoinExclusivity() throws RuleException {
        List<DataSourceJoinInformation> dataSourceJoinInformation = getDataSourceJoinInformation();
        if (null == dataSourceJoinInformation || dataSourceJoinInformation.isEmpty()
                || dataSourceJoinInformation.size() == 1) {
            return;
        }
        for (DataSourceJoinInformation dsji : dataSourceJoinInformation) {
            if (dsji.getTargetDataSource().dataSourceIsMapper()) {
                throw new DataMapperJoinMixedWithAnotherViolationException(this);
            }
        }
    }


    public int rowCount() throws RuleException {
        Context context = lookupContextByDataSourceName(dataSourceName);
        int count;
        if (null != context) {
            count = context.memberValue(context.startSearchPath().toString()).asArray().size();
        } else {
            throw new RuleException("Did not find data source '" + dataSourceName + "' when getting number of rows. "
                    + "Rule component in question is " + name() + ". Make sure data source exists, then try again.");
        }

        return count;
    }


    @Override
    public String dataSourceName() {
        if (!CommonUtils.stringIsBlank(dataSourceName)) {
            return dataSourceName;
        }

        return context().topLevelElementNames().get(0);
    }

    public boolean isFieldValuesAreRegEx() {
        return fieldValuesAreRegEx;
    }
}