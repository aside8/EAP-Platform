package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.EapClient;
import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;
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
    @Setter
    private ScheduledFuture<?> linkTestFuture;

    @Getter
    private final EapClientManager eapClientManager;

    public HsmsClient(ClientOption clientOption, EapClientManager eapClientManager) {
        this.clientOption = clientOption;
        this.eapClientManager = eapClientManager;
        this.selected = false;
        this.group = clientOption.getEventLoopGroup();
    }

    @Override
    public Mono<Void> connect() {
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
                        pipeline.addLast(new LengthField4FrameDecoder());
                        pipeline.addLast(new HsmsMessageDecoder());

                        pipeline.addLast(new HsmsMessageEncoder());
                        pipeline.addLast(new LengthField4FrameEncoder());
                        pipeline.addLast(new HsmsMessageHandler(HsmsClient.this));
                        pipeline.addLast(new Secs2MessageHandler(HsmsClient.this));
                    }
                });

        ChannelFuture future = bootstrap.connect(clientOption.getHost(), clientOption.getPort());
        return Mono.create(sink -> future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = f.channel();
                sink.success();
            } else {
                f.channel().close();
                sink.error(f.cause());
            }
        }));
    }

    @Override
    public Mono<Void> disconnect() {
        return Mono.create(sink -> {
            if (channel != null) {
                channel.close();
            }

            pendingReplies.forEach((id, replySink) -> replySink.error(new IllegalStateException("Client Disconnected")));
            pendingReplies.clear();

            sink.success();
        });
    }


    @Override
    public Flux<Message> receive() {
        return messageSink.asFlux();
    }

    @Override
    public Mono<Void> send(Message message) {
        if (!isConnected()) {
            return Mono.error(new IllegalStateException("Not connected"));
        }

        if (!(message instanceof HsmsMessage hsmsMessage)) {
            return Mono.error(new IllegalArgumentException("Request must be an instance of HsmsMessage"));
        }

        if (hsmsMessage.isRequestMsg()) {
            hsmsMessage.setSystemBytes(systemBytesGenerator.get());
            if (!hsmsMessage.isControlMsg()) {
                hsmsMessage.setDeviceId(clientOption.getDeviceId());
            }
        }

        return Mono.create(sink -> {
            channel.writeAndFlush(hsmsMessage).addListener(f -> {
                if (f.isSuccess()) {
                    sink.success();
                } else {
                    sink.error(f.cause());
                }
            });
        });
    }

    @Override
    public Mono<Message> sendRequest(Message request) {
        if (!isConnected()) {
            return Mono.error(new IllegalStateException("Not connected"));
        }

        if (!(request instanceof HsmsMessage hsmsRequest)) {
            return Mono.error(new IllegalArgumentException("Request must be an instance of HsmsMessage"));
        }

        return Mono.<HsmsMessage>create(sink -> {
            int systemBytes = systemBytesGenerator.incrementAndGet();
            hsmsRequest.setSystemBytes(systemBytes);
            sink.onDispose(() -> pendingReplies.remove(systemBytes));
            pendingReplies.put(systemBytes, sink);
            channel.writeAndFlush(hsmsRequest).addListener(future -> {
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
