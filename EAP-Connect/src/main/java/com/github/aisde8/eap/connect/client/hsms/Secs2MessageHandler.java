package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SECSII;
import com.github.aside8.eap.protocol.secs2.Secs2Constants.*;
import com.github.aside8.eap.protocol.hsms.enums.HsmsMessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Secs2MessageHandler extends SimpleChannelInboundHandler<HsmsMessage> {

    private static final Logger logger = LoggerFactory.getLogger(Secs2MessageHandler.class);

    private HsmsClient hsmsClient;

    public Secs2MessageHandler(HsmsClient hsmsClient) {
        this.hsmsClient = hsmsClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HsmsMessage msg) throws Exception {
        if (msg.getMessageType() == HsmsMessageType.DATA_MESSAGE) {
            onDataMessage(ctx, msg);
        } else {
            // This handler only processes DATA_MESSAGE, pass others along
            ctx.fireChannelRead(msg);
        }
    }

    private void onDataMessage(ChannelHandlerContext ctx, HsmsMessage msg) {
        int systemBytes = msg.getSystemBytes();
        Sinks.One<HsmsMessage> sink = hsmsClient.getPendingReplies().remove(systemBytes);
        if (sink != null) {
            var result = sink.tryEmitValue(msg);
            if (result.isFailure()) {
                logger.debug("reply sink emission failed for {}: {}", systemBytes, result);
            }
            return;
        }

        String streamFunction = msg.getStreamFunction();
        switch (streamFunction) {
            case "S1F1" -> onS1F1Req(ctx, msg);
            case "S1F13" -> onS1F13Req(ctx, msg);
            case "S1F15" -> onS1F15Req(ctx, msg);
            case "S1F17" -> onS1F17Req(ctx, msg);
            case "S2F17" -> onS2F17Req(ctx, msg);
            case "S5F1" -> onS5F1Req(ctx, msg);
            case "S6F1", "S6F3", "S6F11", "S6F13" -> onS6FxReq(ctx, msg);
            case "S10F1", "S10F3", "S10F5" -> onS10FxReq(ctx, msg);
            default -> logger.warn("Unregistered stream function: {}", streamFunction);
        }
        hsmsClient.getMessageSink().tryEmitNext(msg);
    }

    private void onS1F1Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.list());
        ctx.writeAndFlush(dataRsp);
    }

    private void onS1F13Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg,
                SECSII.list(
                        SECSII.binary(COMMACK.OK.getCode()), // COMMACK = 0, OK
                        SECSII.list() // Empty list for MDLN and SOFTREV
                )
        );
        ctx.writeAndFlush(dataRsp);
    }

    private void onS1F15Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S1F16: OFLACK, 0 = OK
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.binary(OFLACK.OK.getCode()));
        ctx.writeAndFlush(dataRsp);
    }

    private void onS1F17Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S1F18: ONLACK, 0 = OK
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.binary(ONLACK.OK.getCode()));
        ctx.writeAndFlush(dataRsp);
    }

    private void onS2F17Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S2F18: YYYYMMDDHHMMSS
        String formattedDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.ascii(formattedDateTime));
        ctx.writeAndFlush(dataRsp);
    }

    private void onS5F1Req(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S5F2: <ACKC5>
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.binary(ACKC5.OK.getCode()));
        ctx.writeAndFlush(dataRsp);
    }

    private void onS6FxReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S6F2, S6F4, S6F12, S6F14: <ACKC6>
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.binary(ACKC6.OK.getCode()));
        ctx.writeAndFlush(dataRsp);
    }

    private void onS10FxReq(ChannelHandlerContext ctx, HsmsMessage msg) {
        // S10F2, S10F4, S10F6: <ACKC10>
        HsmsMessage dataRsp = HsmsMessages.dataRes(msg, SECSII.binary(ACKC10.OK.getCode()));
        ctx.writeAndFlush(dataRsp);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("SECS-II Handler caught exception: {}", cause.getMessage(), cause);
        // We can decide if we need to close the connection or just log the error
        // For now, let's just log and let the HsmsClientLogicHandler handle channel closing
        ctx.fireExceptionCaught(cause);
    }
}
