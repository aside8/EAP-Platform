package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class HsmsClientLogicHandler extends SimpleChannelInboundHandler<HsmsMessage> {

    private static final Logger logger = LoggerFactory.getLogger(HsmsClientLogicHandler.class);

    private final Map<Integer, MonoSink<HsmsMessage>> pendingReplies;

    private final Sinks.Many<Message> messageSink;

    private final AtomicInteger systemBytesGenerator;

    private ScheduledFuture<?> linkTestFuture;

    public HsmsClientLogicHandler(Map<Integer, MonoSink<HsmsMessage>> pendingReplies,
                                  Sinks.Many<Message> messageSink,
                                  AtomicInteger systemBytesGenerator) {

        this.pendingReplies = pendingReplies;
        this.messageSink = messageSink;
        this.systemBytesGenerator = systemBytesGenerator;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HsmsMessage msg) throws Exception {
        int systemBytes = msg.getSystemBytes();
        MonoSink<HsmsMessage> sink = pendingReplies.remove(systemBytes);
        if (sink != null) {
            // 找到了对应的请求，完成 Mono
            sink.success(msg);
        } else {
            // 没找到，这是一个服务器主动推送的非请求消息
            messageSink.tryEmitNext(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HSMS Channel Active: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());

        // 发送 SELECT_REQ 消息
        ctx.writeAndFlush(HsmsMessages.selectReq(systemBytesGenerator.incrementAndGet()));

        // 启动定时器发送 LINK_TEST_REQ 消息
        linkTestFuture = ctx.executor().scheduleAtFixedRate(() -> ctx.writeAndFlush(HsmsMessages.linkTestReq(systemBytesGenerator.incrementAndGet())), 3, 3, TimeUnit.SECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HSMS Channel Inactive: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());

        // 取消定时器
        if (linkTestFuture != null) {
            linkTestFuture.cancel(false);
        }

        // 连接断开时，所有等待中的 Mono 都应失败
        pendingReplies.forEach((id, sink) -> sink.error(new ChannelException("Channel disconnected unexpectedly.")));
        pendingReplies.clear();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HSMS Client Handler caught exception: {}", cause.getMessage(), cause);

        // 异常发生时，所有等待中的 Mono 都应失败
        pendingReplies.forEach((id, sink) -> sink.error(cause));
        pendingReplies.clear();
        ctx.close();
    }
}
