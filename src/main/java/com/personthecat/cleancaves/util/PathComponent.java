package com.personthecat.cleancaves.util;

/**
 * One step of a dotted path into a JSON document -- either an object key or an array index.
 */
public sealed interface PathComponent {

    static PathComponent key(String key) {
        return new Key(key);
    }

    static PathComponent index(int index) {
        return new Index(index);
    }

    record Key(String key) implements PathComponent {}

    record Index(int index) implements PathComponent {}
}
