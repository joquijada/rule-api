package com.exsoinn.ie.rule;

import com.exsoinn.ie.util.CommonUtils;
import com.exsoinn.util.epf.Context;
import com.exsoinn.util.epf.Filter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Created by QuijadaJ on 8/10/2017.
 */
public class QueryableLookupRule extends QueryableDataRule {
    static final String LIST_NAME_FLD_NAME = "list_name";
    static final String LIST_ENTRY_FLD_NAME = "entry";
    static final String LIST_MATCH_OPTION = "matchOption";
    static final String LIST_OUT_FIELD = "outField";
    static final String LIST_SEARCH_FIELD = "searchField";
    private static final String commonPattern = ":(" + "[^-]+" + ")[-\\n\\r]?";
    private static final Pattern matchStylePattern =
            Pattern.compile(LIST_MATCH_OPTION + commonPattern);
    private static final Pattern outFieldPattern =
            Pattern.compile(LIST_OUT_FIELD + commonPattern);
    private static final Pattern searchFieldPattern =
            Pattern.compile(LIST_SEARCH_FIELD + commonPattern);
    private final List<String> listNames;
    private final Map<String, LookupRuleExecutor.LookupRuleExecutionResult> lookupResultCache = new ConcurrentHashMap<>();


    QueryableLookupRule(Object pObj, String pUniqueName, boolean pFldValsAreRegEx, List<String> pOutFlds)
            throws RuleException {
        super(pObj, pUniqueName, pFldValsAreRegEx, pOutFlds);
        listNames = listNames();
    }

    Context assembleListContext(String pListName, Map<String, String> pExtraParams) throws RuleException {
        ensureListOwner(pListName);

        boolean interrupted = false;

        /**
         * The key used to store assembled list context in cache is a combination of the owner component name and
         * the list name. This is because as per current implementation, list names need not be unique outside owning
         * component.
         */
        StringBuilder sb = new StringBuilder(name());
        sb.append("-");
        sb.append(pListName);
        String key = sb.toString();
        Context context = listAssembledContextCache.get(key);
        if (null == context) {
            try {
                StringBuilder listFilter = new StringBuilder();
                listFilter.append(LIST_NAME_FLD_NAME);
                listFilter.append("=");
                listFilter.append(pListName);
                logDebug("Did not find in cache context for lookup list " + pListName + ". Assembling context which "
                        + "will be used for lookup operation, context name is "
                        + this.dataSourceName() + ", rule component name is '" + this.name()
                        + "' and list name is " + pListName);
                context = this.assembleRuleConfigurationData(Filter.valueOf(listFilter.toString()), pExtraParams);
                if (null == context) {
                    throw new RuleException("List with name " + pListName + " could not be found off of component "
                            + this.name() + ". Please check configurations and try again. Did you mistype "
                            + "list name, or was list name changed recently yet dependent rule configs not updated???");
                }

                if (!entryRowCountCacheThresholdMet(this)) {
                    logDebug("Assembled context of list/component " + pListName + "/" + this.name()
                            + " will not get stored in cache because its entry data source does not meet configured row count "
                            + "threshold for caching.");
                    return context;
                }

                Context prevCtx = listAssembledContextCache.putIfAbsent(key, context);
                if (null != prevCtx) {
                    /*
                     * Another thread beat this one to it and stored the list context
                     * first, use that one instead. If "prevCtx" had been NULL it means we were the winners.
                     */
                    context = prevCtx;
                }
            } catch (InterruptedException e) {
                interrupted = true;
                throw new RuleException(e.toString(), e);
            } finally {
                if (interrupted) {
                    // Restore thread interrupted status so that thread owning code can handle appropriately
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            logDebug("Assembled list lookup context found in cache, component name is " + this.name()
                    + ", list name is " + pListName);
        }

        return context;
    }

    String searchField(String pListName) throws RuleException {
        return extractListOption(pListName, LIST_SEARCH_FIELD, searchFieldPattern);
    }

    /**
     * From list name see if a list match option has been specified.
     * @param pListName
     * @return - Match option string, if any match option specified in the list name
     */
    String matchStyle(String pListName) throws RuleException {
        return extractListOption(pListName, LIST_MATCH_OPTION, matchStylePattern);
    }


    /**
     * The output field name is computed as follows:
     * 1) Check if list name specifies an output field to use, and take that one if that's case
     * 2) If not, then check if list name specifies a search field, and use that one
     * 3) If both above fail, then by default just use the field named "entry"
     *
     * @param pListName
     * @return
     * @throws RuleException
     */
    String outField(String pListName) throws RuleException {
        String s;
        if (CommonUtils.stringIsBlank(s = extractListOption(pListName, LIST_OUT_FIELD, outFieldPattern))
                && CommonUtils.stringIsBlank(s = extractListOption(pListName, LIST_SEARCH_FIELD, searchFieldPattern))) {
            s = LIST_ENTRY_FLD_NAME;
        }
        return s;
    }

    private String extractListOption(String pListName, String pOptName, Pattern pPatt) throws RuleException {
        ensureListOwner(pListName);
        if (pListName.indexOf(pOptName) < 0) {
            return null;
        }

        Matcher m = pPatt.matcher(pListName);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }



    List<String> listNames() {
        List<Context> rows = context().memberValue(context().startSearchPath().toString()).asArray();
        return rows.stream().map(e -> e.memberValue(LIST_NAME_FLD_NAME).stringRepresentation())
                .collect(Collectors.toList());
    }


    /**
     * Same as {@link QueryableLookupRule#associatedEntryComponent()}, but this one returns
     * the associated entry data source name instead.
     * @return
     * @throws RuleException
     */
    String associatedEntryDataSourceName() throws RuleException {
        return associatedEntryComponent().dataSourceName();
    }


    /**
     * Finds and returns the {@link IQueryableDataRule} entry component associated with this one. If no associated
     * entry component found then this method will throw {@link RuleException}.
     * @return
     * @throws RuleException
     */
    IQueryableDataRule associatedEntryComponent() throws RuleException {
        for (DataSourceJoinInformation dsji : this.getDataSourceJoinInformation()) {
            IQueryableDataRule joinTarget = dsji.getTargetDataSource();
            if (!joinTarget.dataSourceIsMapper() || null == joinTarget.getDataSourceJoinInformation()) {
                continue;
            }
            for (DataSourceJoinInformation targetDsji : joinTarget.getDataSourceJoinInformation()) {
                IQueryableDataRule joinTargetTarget = targetDsji.getTargetDataSource();
                if (!dataSourceIsEntryContainer(joinTargetTarget)) {
                    continue;
                }

                return joinTargetTarget;
            }
        }


        /**
         * To make things more robust, throw exception if we reach here, to alert user of possible
         * misconfiguration, as all list components are expected to be associated with an entry data source.
         */
        throw new RuleException("Did not find the entry component associated with list component " + this.name()
                + ". Ensure that the configurations are correct.");
    }


    private void ensureListOwner(String pListName) throws RuleException {
        if (!listNames.contains(pListName)) {
            throw new ListNotFoundException(pListName, this);
        }
    }


    /**
     * Validates that this component is associated with exactly one and only one entry data source. Note that it might not
     * be possible to ever see more than one entry data source association, because {@link QueryableDataRule#validateRows()}
     * takes care of enforcing that all rows have identical set of fields, and in order to achieve having a list
     * associated with more than one entry data source, would require the mapper having different fields across rows,
     * or that a data source that joins with a mapper also joins with a non-mapper data source, which is disallowed
     * via {@link QueryableDataRule#validateDataMapperJoinExclusivity()}.
     *
     * @throws RuleException
     */
    void validateNotExactlyOneAssociatedEntryComponent() throws RuleException {
        int entryComponentCnt = 0;
        for (DataSourceJoinInformation dsji : this.getDataSourceJoinInformation()) {
            IQueryableDataRule joinTarget = dsji.getTargetDataSource();
            if (!joinTarget.dataSourceIsMapper() || null == joinTarget.getDataSourceJoinInformation()) {
                continue;
            }
            for (DataSourceJoinInformation targetDsji : joinTarget.getDataSourceJoinInformation()) {
                IQueryableDataRule joinTargetTarget = targetDsji.getTargetDataSource();
                if (!dataSourceIsEntryContainer(joinTargetTarget)) {
                    continue;
                }

                entryComponentCnt++;
            }
        }

        if (entryComponentCnt != 1) {
            throw new NotExactlyOneEntryComponentAssociationException(this);
        }
    }


    /**
     * Checks that this component manages nothing but unique list names. The algorithm to do so is as follows:
     *   - Get list names *as is* (including duplicates)
     *   - Get unique list names (I.e. a set w/o duplicates)
     *   - If size of two above does not match, then automatically not all list names are unique
     * @throws DuplicateListNameException - Thrown if the lists managed by this component do not all have unique names.
     */
    void validateUniqueListNames() throws DuplicateListNameException {
        List<Context> rows = context().memberValue(context().startSearchPath().toString()).asArray();
        List<String> listNames = rows.stream().map(e -> e.memberValue(LIST_NAME_FLD_NAME).stringRepresentation())
                .collect(Collectors.toList());
        Set<String> uniqueListNames = rows.stream().map(e -> e.memberValue(LIST_NAME_FLD_NAME).stringRepresentation())
                .collect(Collectors.toSet());

        /**
         * Oh, oh, the configuration does not have all unique list names. Abort and report
         * on the offending list names, so that user can take corrective action
         */
        if (listNames.size() != uniqueListNames.size()) {
            for (String l : uniqueListNames) {
                listNames.remove(l);
            }
            throw new DuplicateListNameException(listNames, this);
        }
    }


    /**
     * Overridden to add list data source specific validations on top
     * of the parent class ones.
     * @throws RuleException
     */
    @Override
    void performPostJoinInitializationValidations() throws RuleException {
        super.performPostJoinInitializationValidations();
        validateNotExactlyOneAssociatedEntryComponent();
        validateUniqueListNames();
    }

    LookupRuleExecutor.LookupRuleExecutionResult checkAndRetrieveFromCache(String pCacheKey) {
        return lookupResultCache.get(pCacheKey);
    }

    void cacheResult(String pCacheKey, LookupRuleExecutor.LookupRuleExecutionResult pRes) {
        lookupResultCache.put(pCacheKey, pRes);
    }
}
