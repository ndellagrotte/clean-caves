package com.personthecat.cleancaves.proxy;

import java.util.Optional;

/** The dedicated-server side, where there is no local chat GUI to configure. */
public class CommonProxy implements IProxy {

    @Override
    public Optional<String> chatHeight(String[] args) {
        return Optional.empty();
    }

    @Override
    public Optional<String> chatWidth(String[] args) {
        return Optional.empty();
    }

}
