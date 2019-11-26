package io.slingr.api.common;

/**
 * <p>Interface to implement in order to interact with the Json generic implementations
 *
 * <p>Created by lefunes on 20/05/15.
 */
public interface JsonParameter {
    String getName();
    Class getClazz();
    boolean isList();
}
