package com.github.aisde8.eap.connect.server.hsms;

import com.github.aisde8.eap.connect.client.EapClient;
import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aisde8.eap.connect.client.hsms.*;
import com.github.aisde8.eap.connect.server.EapServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;

public class HsmsServer implements EapServer {

    private static final Logger logger = LoggerFactory.getLogger(HsmsServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ServerOption option ;

    private EapClientManager eapClientManager;

    private final Sinks.Many<EapClient> clientSink = Sinks.many().multicast().onBackpressureBuffer();

    public HsmsServer(ServerOption option, EapClientManager eapClientManager) {
        this.option = option;
        this.eapClientManager = eapClientManager;
    }

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new  NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new  ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        InetSocketAddress remoteAddress = ch.remoteAddress();
                        String host = remoteAddress.getHostString();
                        int port = remoteAddress.getPort();

                        ClientOption clientOption = ClientOption.builder()
                                .host(host)
                                .port(port)
                                .deviceId(0) // Default deviceId, should be updated after select.req
                                .eventLoopGroup(workerGroup)
                                .build();

                        HsmsClient hsmsClient = new HsmsClient(ch, clientOption, eapClientManager);

                        eapClientManager.addClient(host, port, hsmsClient);
                        clientSink.tryEmitNext(hsmsClient);

                        ch.pipeline().addLast(new LengthField4FrameDecoder());
                        ch.pipeline().addLast(new HsmsMessageDecoder());
                        ch.pipeline().addLast(new HsmsMessageEncoder());
                        ch.pipeline().addLast(new LengthField4FrameEncoder());
                        ch.pipeline().addLast(new HsmsMessageHandler(hsmsClient));
                        ch.pipeline().addLast(new Secs2MessageHandler(hsmsClient));
                    }
                });

        try {
            ChannelFuture f = serverBootstrap.bind(option.getPort()).sync();
            logger.info("HSMS server started on port {}", option.getPort());
            f.channel().closeFuture().addListener(future -> shutdown());
        } catch (InterruptedException e) {
            logger.error("HSMS server start failed", e);
            Thread.currentThread().interrupt();
            shutdown();
        }
    }

    @Override
    public Flux<EapClient> accept() {
        return clientSink.asFlux();
    }

    @Override
    public void shutdown() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        clientSink.tryEmitComplete();
        logger.info("HSMS server shut down");
    }
}
