package com.personthecat.cleancaves;

/**
 * Mod identity constants. The {@code @{...}} markers are Token Envoy tokens, replaced at build time from
 * {@code tokens.properties} and {@code build.gradle}.
 */
public final class Reference {

    public static final String MOD_ID = "@{MOD_ID}";
    public static final String MOD_NAME = "@{MOD_NAME}";
    public static final String VERSION = "@{MOD_VERSION}";

    private Reference() { }

}
