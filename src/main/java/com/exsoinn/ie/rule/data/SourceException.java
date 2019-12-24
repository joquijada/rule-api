package com.exsoinn.ie.rule.data;

/**
 * Created by QuijadaJ on 11/16/2017.
 */
public class SourceException extends Exception {
    SourceException(Throwable pThrown) {
        super(pThrown);
    }

    SourceException(String pMsg) {
        super(pMsg);
    }
}