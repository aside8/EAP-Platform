package com.github.aisde8.eap.connect.client;

import com.github.aside8.eap.protocol.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Redesigned client API with clearer semantics and richer metadata.
 *
 * - New methods express intent (Mono<Void> for completion vs Boolean ambiguous success flag).
 * - Backward-compatible adapters are provided and marked {@code @Deprecated} so existing callers keep working.
 */
public interface EapClient extends AutoCloseable {

    Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /* ---------- New, preferred API ---------- */

    /**
     * Connect to remote peer. Completes when the connection is established.
     * Errors if connection cannot be established within the provided timeout (if honored by implementation).
     */
    Mono<Void> connect(Duration timeout);

    /**
     * Disconnect and release resources. Completes when graceful shutdown finishes.
     * Implementations may honor the provided timeout.
     */
    Mono<Void> disconnect(Duration timeout);

    /**
     * Hot stream of lifecycle events (CONNECTED / DISCONNECTED / ERROR).
     * Implementations must not complete this Flux under normal operation.
     */
    Flux<ConnectionEvent> connectionEvents();

    /**
     * Hot inbound message stream that includes transport/protocol metadata.
     */
    Flux<InboundMessage> receiveWithMeta();

    /**
     * Fire-and-forget send. Completion means the message was written/flushed to the transport.
     */
    Mono<Void> sendVoid(Message message);

    /**
     * Request/response with explicit timeout and reply metadata.
     */
    Mono<Reply<Message>> sendRequest(Message request, Duration timeout);

    /**
     * Instant snapshot of connection state. Prefer subscribing to {@link #connectionEvents()} for reliable state transitions.
     */
    boolean isConnected();

    /* ---------- Backward-compatible adapters (deprecated) ---------- */

    /**
     * @deprecated use {@link #connect(Duration)}
     */
    @Deprecated
    default Mono<Boolean> connect() {
        return connect(DEFAULT_TIMEOUT).thenReturn(Boolean.TRUE);
    }

    /**
     * @deprecated use {@link #disconnect(Duration)}
     */
    @Deprecated
    default Mono<Boolean> disconnect() {
        return disconnect(DEFAULT_TIMEOUT).thenReturn(Boolean.TRUE);
    }

    /**
     * @deprecated use {@link #receiveWithMeta()} and map to payload
     */
    @Deprecated
    default Flux<Message> receive() {
        return receiveWithMeta().map(InboundMessage::getPayload);
    }

    /**
     * @deprecated use {@link #sendVoid(Message)}
     */
    @Deprecated
    default Mono<Boolean> send(Message message) {
        return sendVoid(message).thenReturn(Boolean.TRUE);
    }

    /**
     * @deprecated use {@link #sendRequest(Message, Duration)}
     */
    @Deprecated
    default Mono<Message> sendRequest(Message request) {
        return sendRequest(request, DEFAULT_TIMEOUT).map(Reply::getPayload);
    }

    /**
     * Close is equivalent to {@link #disconnect()} and is provided for try-with-resources compatibility.
     */
    @Override
    default void close() {
        disconnect().block();
    }
}
