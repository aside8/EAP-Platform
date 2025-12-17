package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.*;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class HsmsMessageHandler extends SimpleChannelInboundHandler<HsmsMessage> {

    private static final Logger logger = LoggerFactory.getLogger(HsmsMessageHandler.class);

    private final HsmsClient hsmsClient;

    private ScheduledFuture<?> linkTestFuture;

    public HsmsMessageHandler(HsmsClient hsmsClient) {
        this.hsmsClient = hsmsClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HsmsMessage msg) throws Exception {
        // Handle HSMS-specific messages and pass DATA_MESSAGE to the next handler
        switch (msg.getMessageType()) {
            case DATA_MESSAGE -> ctx.fireChannelRead(msg);
            case SELECT_REQ -> onSelectReq(ctx, msg);
            case SELECT_RSP -> onSelectRsp(ctx, msg);
            case DESELECT_REQ -> onDeselectReq(ctx, msg);
            case DESELECT_RSP -> onDeselectRsp(ctx, msg);
            case LINK_TEST_REQ -> onLinkTestReq(ctx, msg);
            case LINK_TEST_RSP -> onLinkTestRsp(ctx, msg);
            case ABORT_REQ -> onAbortReq(ctx, msg);
            default -> logger.warn("Received unknown HSMS message type: {}", msg.getMessageType());
        }

        // Emit control messages to the sink if needed by other parts of the application
        if (msg.getMessageType() != HsmsMessageType.DATA_MESSAGE) {
            hsmsClient.getMessageSink().tryEmitNext(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HSMS Channel Active: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        hsmsClient.getEapClientManager().addClient(hsmsClient.getClientOption().getHost(), hsmsClient.getClientOption().getPort(), hsmsClient);
        hsmsClient.setSelected(false);

        // Send SELECT_REQ message
        var generator = hsmsClient.getSystemBytesGenerator();
        ctx.writeAndFlush(HsmsMessages.selectReq(hsmsClient.getClientOption().getDeviceId(), generator.incrementAndGet()));

        // Start a timer to send LINK_TEST_REQ messages every 10 seconds
        this.linkTestFuture = ctx.executor().scheduleAtFixedRate(() ->
                ctx.writeAndFlush(HsmsMessages.linkTestReq(generator.incrementAndGet())), 10, 10, TimeUnit.SECONDS);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HSMS Channel Inactive: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        hsmsClient.setSelected(false);

        // Cancel the timer
        if (linkTestFuture != null) {
            linkTestFuture.cancel(false);
        }

        // All pending Monos should fail when the connection is lost
        hsmsClient.getPendingReplies().forEach((id, sink) -> sink.error(new ChannelException("Channel disconnected unexpectedly.")));
        hsmsClient.getPendingReplies().clear();

        hsmsClient.getEapClientManager().removeClient(hsmsClient.getClientOption().getHost(), hsmsClient.getClientOption().getPort());
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("HSMS Client Handler caught exception: {}", cause.getMessage(), cause);
        hsmsClient.setSelected(false);

        // All pending Monos should fail on exception
        hsmsClient.getPendingReplies().forEach((id, sink) -> sink.error(cause));
        hsmsClient.getPendingReplies().clear();
        ctx.close();
    }

    private void onSelectReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        if (!hsmsClient.isSelected()) {
            hsmsClient.setSelected(true);
            HsmsMessage selectRes = HsmsMessages.selectResp(msg, SelectStatus.CONNECTION_ESTABLISHED);
            ctx.writeAndFlush(selectRes);
            logger.info("Received SELECT_REQ on a client not yet selected: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            HsmsMessage selectRes = HsmsMessages.selectResp(msg, SelectStatus.ALREADY_SELECTED);
            ctx.writeAndFlush(selectRes);
            logger.warn("Received unexpected SELECT_REQ on a client already selected: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }
    }

    private void onSelectRsp(ChannelHandlerContext ctx, HsmsMessage msg) {
        HsmsHeader header = msg.getHeader();
        SelectStatus selectStatus = SelectStatus.valueOf(header.getFunction());
        if (selectStatus == SelectStatus.CONNECTION_ESTABLISHED || selectStatus == SelectStatus.ALREADY_SELECTED) {
            hsmsClient.setSelected(true);
            logger.info("HSMS Session Established: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            logger.warn("HSMS Session Failed to Establish with status [{}]: {} -> {}", msg.getHeader().getPtype(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }
    }

    private void onDeselectReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        if (hsmsClient.isSelected()) {
            hsmsClient.setSelected(false);
            HsmsMessage deselectRes = HsmsMessages.deselectResp(msg, DeselectStatus.COMMUNICATION_END);
            ctx.writeAndFlush(deselectRes);
            logger.info("Received deselectReq on a client selected: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            HsmsMessage deselectRes = HsmsMessages.deselectResp(msg, DeselectStatus.COMMUNICATION_NOT_ESTABLISHED);
            ctx.writeAndFlush(deselectRes);
            logger.warn("Received unexpected deselectReq on a client not selected: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }
    }

    private void onDeselectRsp(ChannelHandlerContext ctx, HsmsMessage msg) {
        DeselectStatus deselectStatus = DeselectStatus.valueOf(msg.getHeader().getFunction());
        if (deselectStatus == DeselectStatus.COMMUNICATION_END || deselectStatus == DeselectStatus.COMMUNICATION_NOT_ESTABLISHED) {
            hsmsClient.setSelected(false);
            logger.info("HSMS Session deselected successfully: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            logger.warn("HSMS Session deselected Failed with status [{}]: {} -> {}", msg.getHeader().getPtype(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }
    }

    // 收到LINK_TEST_REQ时，回复LINK_TEST_RSP
    private void onLinkTestReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        HsmsMessage linkTestRes = HsmsMessages.linkTestResp(msg);
        ctx.writeAndFlush(linkTestRes);
    }

    // 收到LINK_TEST_RSP时，忽略
    private void onLinkTestRsp(ChannelHandlerContext ctx, HsmsMessage msg) {
    }

    // 收到ABORT_REQ时，关闭连接
    private void onAbortReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        ctx.close();
    }
}