package com.github.aside8.eap.protocol;

public enum Protocol {
    HSMS("HSMS"),
    ;
    private final String name;

    Protocol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
