package com.github.aisde8.eap.connect.client;

import java.time.Duration;

public final class Reply<T> {
    private final T payload;
    private final Duration rtt;

    public Reply(T payload, Duration rtt) {
        this.payload = payload;
        this.rtt = rtt;
    }

    public T getPayload() { return payload; }
    public Duration getRtt() { return rtt; }
}
