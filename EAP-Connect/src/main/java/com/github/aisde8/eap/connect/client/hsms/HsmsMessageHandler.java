package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.hsms.SelectStatus;
import com.github.aside8.eap.protocol.hsms.HsmsMessageType;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.MonoSink;

import java.util.concurrent.TimeUnit;

public class HsmsMessageHandler extends SimpleChannelInboundHandler<HsmsMessage> {

    private static final Logger logger = LoggerFactory.getLogger(HsmsMessageHandler.class);

    private final HsmsClient hsmsClient;

    public HsmsMessageHandler(HsmsClient hsmsClient) {
        this.hsmsClient = hsmsClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HsmsMessage msg) throws Exception {
        int systemBytes = msg.getSystemBytes();
        MonoSink<HsmsMessage> sink = hsmsClient.getPendingReplies().remove(systemBytes);
        if (sink != null) {
            // Found a matching request, complete the Mono
            sink.success(msg);
            return;
        }

        // Handle HSMS-specific messages and pass DATA_MESSAGE to the next handler
        switch (msg.getMessageType()) {
            case DATA_MESSAGE:
                // Pass SECS-II messages to the next handler in the pipeline
                ctx.fireChannelRead(msg);
                break;
            case SELECT_REQ:
                onSelectReq(ctx, msg);
                break;
            case SELECT_RSP:
                onSelectRsp(ctx, msg);
                break;
            case DESELECT_REQ:
                onDeselectReq(ctx, msg);
                break;
            case DESELECT_RSP:
                onDeselectRsp(ctx, msg);
                break;
            case LINK_TEST_REQ:
                onLinkTestReq(ctx, msg);
                break;
            case LINK_TEST_RSP:
                onLinkTestRsp(ctx, msg);
                break;
            case ABORT_REQ:
                onAbortReq(ctx, msg);
                break;
            default:
                logger.warn("Received unknown HSMS message type: {}", msg.getMessageType());
                break;
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

        // Send SELECT_REQ message
        var generator = hsmsClient.getSystemBytesGenerator();
        ctx.writeAndFlush(HsmsMessages.selectReq(generator.incrementAndGet()));

        // Start a timer to send LINK_TEST_REQ messages
        var linkTestFuture = ctx.executor().scheduleAtFixedRate(() ->
                        ctx.writeAndFlush(HsmsMessages.linkTestReq(generator.incrementAndGet())), 3, 3, TimeUnit.SECONDS);
        hsmsClient.setLinkTestFuture(linkTestFuture);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("HSMS Channel Inactive: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());

        // Cancel the timer
        if (hsmsClient.getLinkTestFuture() != null) {
            hsmsClient.getLinkTestFuture().cancel(false);
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

        // All pending Monos should fail on exception
        hsmsClient.getPendingReplies().forEach((id, sink) -> sink.error(cause));
        hsmsClient.getPendingReplies().clear();
        ctx.close();
    }

    private void onSelectReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        if (hsmsClient.isSelected()) {
            HsmsMessage selectRes = HsmsMessages.selectResp(msg, SelectStatus.ALREADY_SELECTED);
            ctx.writeAndFlush(selectRes);
            logger.warn("Received SELECT_REQ on an already selected client: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            HsmsMessage selectRes = HsmsMessages.selectResp(msg, SelectStatus.CONNECTION_NOT_READY);
            ctx.writeAndFlush(selectRes);
            logger.warn("Received unexpected SELECT_REQ on a client not yet selected: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        }
    }

    private void onSelectRsp(ChannelHandlerContext ctx, HsmsMessage msg) {
        if (msg.getHeader().getPtype() == SelectStatus.CONNECTION_ESTABLISHED.getCode()) {
            hsmsClient.setSelected(true);
            logger.info("HSMS Session Established: {} -> {}", ctx.channel().localAddress(), ctx.channel().remoteAddress());
        } else {
            logger.warn("HSMS Session Failed to Establish with status [{}]: {} -> {}", msg.getHeader().getPtype(), ctx.channel().localAddress(), ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    private void onDeselectReq(ChannelHandlerContext ctx, HsmsMessage msg) {}

    private void onDeselectRsp(ChannelHandlerContext ctx, HsmsMessage msg) {}

    private void onLinkTestReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        HsmsMessage linkTestRes = HsmsMessages.linkTestResp(msg);
        ctx.writeAndFlush(linkTestRes);
    }

    private void onLinkTestRsp(ChannelHandlerContext ctx, HsmsMessage msg) {}

    private void onAbortReq(ChannelHandlerContext ctx, HsmsMessage msg) {}
}