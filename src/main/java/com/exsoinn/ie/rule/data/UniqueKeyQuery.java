package com.exsoinn.ie.rule.data;

/**
 * Created by QuijadaJ on 9/27/2017.
 * TODO: If key is made up of more than one member (I.e. a composite key), need a different child class
 */
class UniqueKeyQuery implements Query {
    private final String key;


    UniqueKeyQuery(String pKey) {
        key = pKey;
    }


    String key() {
        return key;
    }

}
