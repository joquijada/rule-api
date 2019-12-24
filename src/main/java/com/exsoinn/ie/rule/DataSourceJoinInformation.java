package com.exsoinn.ie.rule;

import net.jcip.annotations.Immutable;

/**
 * Encapsulates the information needed by one data source to join with another data source. Instances
 * of this class are immutable
 * Created by QuijadaJ on 5/24/2017.
 */
@Immutable
class DataSourceJoinInformation {
    private final IQueryableDataRule targetDataSource;
    private final String sourceJoinFieldName;
    private final String targetJoinFieldName;


    /*
     * Constructors
     */
    DataSourceJoinInformation(
            IQueryableDataRule pDataSrc,
            String pSrcJoinFldName) {
        this(pDataSrc, pSrcJoinFldName, QueryableDataRule.ID_FLD_NAME);
    }

    DataSourceJoinInformation(
            IQueryableDataRule pDataSrc,
            String pSrcJoinFldName,
            String pTargetJoinFldName) {
        targetDataSource = pDataSrc;
        sourceJoinFieldName = pSrcJoinFldName;
        targetJoinFieldName = pTargetJoinFldName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Target Join Data Source: ");
        sb.append(targetDataSource.name());
        sb.append("\nSource Data Source Join Field Name (aka foreign key): ");
        sb.append(sourceJoinFieldName);
        sb.append("\nTarget Data Source Join Field Name (the field on the target data source that will join with mine): ");
        sb.append(targetJoinFieldName);

        return sb.toString();
    }


    /*
     * Getters
     */
    IQueryableDataRule getTargetDataSource() {
        return targetDataSource;
    }
    String getSourceJoinFieldName() {
        return sourceJoinFieldName;
    }

    String getTargetJoinFieldName() {
        return targetJoinFieldName;
    }
}
