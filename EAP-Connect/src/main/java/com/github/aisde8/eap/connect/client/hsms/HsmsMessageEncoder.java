package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class HsmsMessageEncoder extends MessageToMessageEncoder<HsmsMessage> {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, HsmsMessage hsmsMessage, List<Object> out) throws Exception {
        out.add(hsmsMessage.encode(channelHandlerContext.alloc()));
    }
}
