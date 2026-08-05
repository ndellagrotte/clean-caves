package com.personthecat.cleancaves.proxy;

import java.util.Optional;

/**
 * Side-specific behaviour. Everything that touches {@code net.minecraft.client.*} lives behind
 * this interface so that the common code never loads a client class on a dedicated server.
 */
public interface IProxy {

    /**
     * Reports or adjusts the local player's chat height.
     *
     * @param args empty to read the current value, otherwise a scale or {@code "max"}.
     * @return the message to show the player, or empty when there is no local client.
     */
    Optional<String> chatHeight(String[] args);

    /** As {@link #chatHeight}, for chat width. */
    Optional<String> chatWidth(String[] args);
}
