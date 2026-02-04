package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.ConnectionEvent;
import com.github.aisde8.eap.connect.client.EapClient;
import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aisde8.eap.connect.client.InboundMessage;
import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HsmsClient implements EapClient {

    private static final Logger logger = LoggerFactory.getLogger(HsmsClient.class);

    private EventLoopGroup group;

    private volatile Channel channel;

    @Getter
    private final ClientOption clientOption;

    @Getter
    private final AtomicInteger systemBytesGenerator = new AtomicInteger(0);

    @Getter
    private final Map<Integer, Sinks.One<HsmsMessage>> pendingReplies = new ConcurrentHashMap<>();

    @Getter
    private final Sinks.Many<Message> messageSink = Sinks.many().multicast().onBackpressureBuffer(1024, false);

    private final Sinks.Many<ConnectionEvent> connectionEventSink = Sinks.many().multicast().onBackpressureBuffer(16, false);

    @Getter
    @Setter
    private boolean selected;

    @Getter
    private final EapClientManager eapClientManager;

    private final boolean ownsEventLoopGroup;

    public HsmsClient(ClientOption clientOption, EapClientManager eapClientManager) {
        this.clientOption = clientOption;
        this.eapClientManager = eapClientManager;
        this.selected = false;

        EventLoopGroup provided = clientOption.getEventLoopGroup();
        if (provided == null) {
            this.group = new NioEventLoopGroup();
            this.ownsEventLoopGroup = true;
        } else {
            this.group = provided;
            this.ownsEventLoopGroup = false;
        }
    }

    public HsmsClient(Channel channel, ClientOption clientOption, EapClientManager eapClientManager) {
        this.channel = channel;
        this.clientOption = clientOption;
        this.eapClientManager = eapClientManager;
        this.selected = false;
        this.group = channel.eventLoop();
        this.ownsEventLoopGroup = false;
    }

    @Override
    public Mono<Boolean> connect() {
        return connect(DEFAULT_TIMEOUT).thenReturn(Boolean.TRUE);
    }

    @Override
    public Mono<Void> connect(Duration timeout) {
        Bootstrap bootstrap = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Math.min(timeout.toMillis(), Integer.MAX_VALUE))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // framing should be configurable because some peers include the 4-byte length in the length field
                        pipeline.addLast("lengthField4FrameDecoder", new LengthField4FrameDecoder(clientOption.isIncludeLength()));
                        pipeline.addLast("lengthField4FrameEncoder", new LengthField4FrameEncoder(clientOption.isIncludeLength()));
                        pipeline.addLast("hsmsMessageDecoder", new HsmsMessageDecoder());
                        pipeline.addLast("hsmsMessageEncoder", new HsmsMessageEncoder());
                        pipeline.addLast("hsmsMessageHandler", new HsmsMessageHandler(HsmsClient.this));
                        pipeline.addLast("secs2MessageHandler", new Secs2MessageHandler(HsmsClient.this));
                    }
                });

        ChannelFuture future = bootstrap.connect(clientOption.getHost(), clientOption.getPort());
        return Mono.<Void>create(sink -> future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                connectionEventSink.tryEmitNext(ConnectionEvent.connected());
                sink.success();
            } else {
                if (f.channel() != null) f.channel().close();
                connectionEventSink.tryEmitNext(ConnectionEvent.error(f.cause()));
                sink.error(f.cause());
            }
        })).timeout(timeout);
    }

    @Override
    public Mono<Boolean> disconnect() {
        return disconnect(DEFAULT_TIMEOUT).thenReturn(Boolean.TRUE);
    }

    @Override
    public Mono<Void> disconnect(Duration timeout) {
        return Mono.<Void>create(sink -> {
            Channel ch = channel;
            if (ch != null) {
                ch.close().addListener((ChannelFutureListener) f -> {
                    pendingReplies.forEach((id, replySink) -> replySink.tryEmitError(new IllegalStateException("Client Disconnected")));
                    pendingReplies.clear();
                    if (ownsEventLoopGroup) {
                        group.shutdownGracefully().addListener(gf -> connectionEventSink.tryEmitNext(ConnectionEvent.disconnected()));
                    } else {
                        connectionEventSink.tryEmitNext(ConnectionEvent.disconnected());
                    }
                    sink.success();
                });
            } else {
                if (ownsEventLoopGroup && group != null) {
                    group.shutdownGracefully().addListener(gf -> connectionEventSink.tryEmitNext(ConnectionEvent.disconnected()));
                } else {
                    connectionEventSink.tryEmitNext(ConnectionEvent.disconnected());
                }
                pendingReplies.forEach((id, replySink) -> replySink.tryEmitError(new IllegalStateException("Client Disconnected")));
                pendingReplies.clear();
                sink.success();
            }
        }).timeout(timeout);
    }

    @Override
    public Flux<ConnectionEvent> connectionEvents() {
        return connectionEventSink.asFlux();
    }


    @Override
    public Flux<Message> receive() {
        return receiveWithMeta().map(InboundMessage::getPayload);
    }

    @Override
    public Flux<InboundMessage> receiveWithMeta() {
        return messageSink.asFlux().map(m -> new InboundMessage(m, Instant.now(), Map.of()));
    }

    @Override
    public Mono<Boolean> send(Message message) {
        if (!isConnected()) {
            return Mono.error(new IllegalStateException("Not connected"));
        }

        if (!(message instanceof HsmsMessage hsmsMessage)) {
            return Mono.error(new IllegalArgumentException("Request must be an instance of HsmsMessage"));
        }

        if (hsmsMessage.isControlMsg()) {
            return Mono.error(new IllegalArgumentException("Control message not supported"));
        }

        hsmsMessage.setDeviceId(clientOption.getDeviceId());
        if (hsmsMessage.isRequestMsg()) {
            // FIX: increment system bytes for each request
            hsmsMessage.setSystemBytes(systemBytesGenerator.incrementAndGet());
        }
        return Mono.<Boolean>create(sink -> {
            ChannelFuture future = channel.writeAndFlush(hsmsMessage);
            future.addListener(f -> {
                if (f.isSuccess()) {
                    sink.success(true);
                } else {
                    sink.error(f.cause());
                }
            });
        }).timeout(Duration.ofSeconds(5));
    }

    @Override
    public Mono<Void> sendVoid(Message message) {
        return send(message).then();
    }

    @Override
    public Mono<Message> sendRequest(Message request) {
        return sendRequest(request, Duration.ofSeconds(5)).map(com.github.aisde8.eap.connect.client.Reply::getPayload);
    }

    @Override
    public Mono<com.github.aisde8.eap.connect.client.Reply<Message>> sendRequest(Message request, Duration timeout) {
        if (!isConnected()) return Mono.error(new IllegalStateException("Not connected"));
        if (!(request instanceof HsmsMessage hsmsRequest)) return Mono.error(new IllegalArgumentException("Request must be an instance of HsmsMessage"));
        if (hsmsRequest.isControlMsg()) return Mono.error(new IllegalArgumentException("Control message not supported"));
        if (!hsmsRequest.isRequestMsg()) return Mono.error(new IllegalArgumentException("Request must be a request message"));
        if (hsmsRequest.isDataMsg() && !hsmsRequest.getHeader().isWbit()) return Mono.error(new IllegalArgumentException("data req message must have W-bit set"));

        int systemBytes = systemBytesGenerator.incrementAndGet();
        hsmsRequest.setDeviceId(clientOption.getDeviceId());
        hsmsRequest.setSystemBytes(systemBytes);

        Sinks.One<HsmsMessage> replySink = Sinks.one();
        pendingReplies.put(systemBytes, replySink);

        channel.writeAndFlush(hsmsRequest).addListener(future -> {
            if (!future.isSuccess()) {
                var removed = pendingReplies.remove(systemBytes);
                if (removed != null) removed.tryEmitError(future.cause());
            }
        });

        return replySink.asMono()
                .doFinally(sig -> pendingReplies.remove(systemBytes))
                .timeout(timeout)
                .map(msg -> new com.github.aisde8.eap.connect.client.Reply<Message>(msg, Duration.ofMillis(0)));
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
}