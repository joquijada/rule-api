package com.exsoinn.ie.rule;

public class DataSourceException extends Exception {

    DataSourceException(String pMsg, Throwable t) {
        super(pMsg, t);
    }

    DataSourceException(String pMsg) {
        this(pMsg, null);
    }

    DataSourceException(Throwable t) {
        super(t);
    }

}
