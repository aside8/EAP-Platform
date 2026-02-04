package com.github.aisde8.eap.connect.client;

import java.time.Instant;
import java.util.Objects;

public final class ConnectionEvent {
    public enum Type { CONNECTED, DISCONNECTED, ERROR }

    private final Type type;
    private final Instant when;
    private final Throwable error; // present only for ERROR

    private ConnectionEvent(Type type, Instant when, Throwable error) {
        this.type = type;
        this.when = when;
        this.error = error;
    }

    public static ConnectionEvent connected() {
        return new ConnectionEvent(Type.CONNECTED, Instant.now(), null);
    }

    public static ConnectionEvent disconnected() {
        return new ConnectionEvent(Type.DISCONNECTED, Instant.now(), null);
    }

    public static ConnectionEvent error(Throwable t) {
        return new ConnectionEvent(Type.ERROR, Instant.now(), Objects.requireNonNull(t));
    }

    public Type getType() { return type; }
    public Instant getWhen() { return when; }
    public Throwable getError() { return error; }
}
