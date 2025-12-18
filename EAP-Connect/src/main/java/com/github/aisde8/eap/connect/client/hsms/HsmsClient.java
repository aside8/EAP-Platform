package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.EapClient;
import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

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
    private final Map<Integer, MonoSink<HsmsMessage>> pendingReplies = new ConcurrentHashMap<>();

    @Getter
    private final Sinks.Many<Message> messageSink = Sinks.many().multicast().onBackpressureBuffer();

    @Getter
    @Setter
    private boolean selected;

    @Getter
    private final EapClientManager eapClientManager;

    public HsmsClient(ClientOption clientOption, EapClientManager eapClientManager) {
        this.clientOption = clientOption;
        this.eapClientManager = eapClientManager;
        this.selected = false;
        this.group = clientOption.getEventLoopGroup();
    }

    public HsmsClient(Channel channel, ClientOption clientOption, EapClientManager eapClientManager) {
        this.channel = channel;
        this.clientOption = clientOption;
        this.eapClientManager = eapClientManager;
        this.selected = false;
        this.group = channel.eventLoop();
    }

    @Override
    public Mono<Boolean> connect() {
        group = clientOption.getEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10 * 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("lengthField4FrameDecoder", new LengthField4FrameDecoder());
                        pipeline.addLast("lengthField4FrameEncoder", new LengthField4FrameEncoder());
                        pipeline.addLast("hsmsMessageDecoder", new HsmsMessageDecoder());
                        pipeline.addLast("hsmsMessageEncoder", new HsmsMessageEncoder());
                        pipeline.addLast("hsmsMessageHandler", new HsmsMessageHandler(HsmsClient.this));
                        pipeline.addLast("secs2MessageHandler", new Secs2MessageHandler(HsmsClient.this));
                    }
                });

        ChannelFuture future = bootstrap.connect(clientOption.getHost(), clientOption.getPort());
        return Mono.create(sink -> future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                sink.success(true);
            } else {
                f.channel().close();
                sink.error(f.cause());
            }
        }));
    }

    @Override
    public Mono<Boolean> disconnect() {
        return Mono.create(sink -> {
            if (channel != null) {
                channel.close();
            }
            pendingReplies.forEach((id, replySink) -> replySink.error(new IllegalStateException("Client Disconnected")));
            pendingReplies.clear();
            sink.success(true);
        });
    }


    @Override
    public Flux<Message> receive() {
        return messageSink.asFlux();
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
            hsmsMessage.setSystemBytes(systemBytesGenerator.get());
        }
        ChannelFuture future = channel.writeAndFlush(hsmsMessage);
        return Mono.create(sink -> future.addListener(f -> {
            if (f.isSuccess()) {
                sink.success(true);
            } else {
                sink.error(f.cause());
            }
        }));
    }

    @Override
    public Mono<Message> sendRequest(Message request) {
        if (!isConnected()) {
            return Mono.error(new IllegalStateException("Not connected"));
        }

        if (!(request instanceof HsmsMessage hsmsRequest)) {
            return Mono.error(new IllegalArgumentException("Request must be an instance of HsmsMessage"));
        }

        if (hsmsRequest.isControlMsg()) {
            return Mono.error(new IllegalArgumentException("Control message not supported"));
        }

        if (!hsmsRequest.isRequestMsg()) {
            return Mono.error(new IllegalArgumentException("Request must be a request message"));
        }

        if (hsmsRequest.isDataMsg() && !hsmsRequest.getHeader().isWbit()) {
            return Mono.error(new IllegalArgumentException("data req message must have W-bit set"));
        }

        int systemBytes = systemBytesGenerator.incrementAndGet();
        hsmsRequest.setDeviceId(clientOption.getDeviceId());
        hsmsRequest.setSystemBytes(systemBytes);
        ChannelFuture requestFuture = channel.writeAndFlush(hsmsRequest);

        return Mono.<HsmsMessage>create(sink -> {
            pendingReplies.put(systemBytes, sink);
            sink.onDispose(() -> pendingReplies.remove(systemBytes));
            requestFuture.addListener(future -> {
                if (!future.isSuccess()) {
                    pendingReplies.remove(systemBytes);
                    sink.error(future.cause());
                }
            });
        }).cast(Message.class);
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
}