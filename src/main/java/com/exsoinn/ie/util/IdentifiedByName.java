package com.exsoinn.ie.util;

/**
 * This interface can be implemented by any entity that wishes to identify itself by a name, be it a unique one
 * or not. One example use case is that an object of this type can simply invoke its {@link IdentifiedByName#name()}
 * method and use that as the key to insert the instance object into a Map, and it does not matter what other class(es)
 * the object is a type of, as long as polymorphism is used to refer to the object by its {@link IdentifiedByName} type.
 *
 * Created by QuijadaJ on 11/15/2017.
 */
public interface IdentifiedByName {
    String name();
}
