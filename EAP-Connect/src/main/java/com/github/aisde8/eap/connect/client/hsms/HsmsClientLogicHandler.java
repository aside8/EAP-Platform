package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.Message;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;

import java.util.Map;

public class HsmsClientLogicHandler extends SimpleChannelInboundHandler<HsmsMessage> {

    private final Map<Integer, MonoSink<HsmsMessage>> pendingReplies;

    private final Sinks.Many<Message> messageSink;

    public HsmsClientLogicHandler(Map<Integer, MonoSink<HsmsMessage>> pendingReplies, Sinks.Many<Message> messageSink) {
        this.pendingReplies = pendingReplies;
        this.messageSink = messageSink;
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
        // 连接建立后可以发送 HSMS 握手消息，例如 SELECT.REQ (S1F13)
        // 为了演示，这里不自动发送，由 DefaultEapClient.connect() 的调用者决定
        System.out.println("HSMS Channel Active: " + ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("HSMS Channel Inactive: " + ctx.channel().remoteAddress());
        // 连接断开时，所有等待中的 Mono 都应失败
        pendingReplies.forEach((id, sink) -> sink.error(new ChannelException("Channel disconnected unexpectedly.")));
        pendingReplies.clear();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("HSMS Client Handler caught exception: " + cause.getMessage());
        // 异常发生时，所有等待中的 Mono 都应失败
        pendingReplies.forEach((id, sink) -> sink.error(cause));
        pendingReplies.clear();
        ctx.close();
    }
}
