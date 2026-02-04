package com.github.aisde8.eap.connect.client;

import com.github.aside8.eap.protocol.Message;

import java.time.Instant;
import java.util.Map;

public final class InboundMessage {
    private final Message payload;
    private final Instant receivedAt;
    private final Map<String, Object> meta;

    public InboundMessage(Message payload, Instant receivedAt, Map<String, Object> meta) {
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.meta = meta;
    }

    public Message getPayload() { return payload; }
    public Instant getReceivedAt() { return receivedAt; }
    public Map<String, Object> getMeta() { return meta; }
}
