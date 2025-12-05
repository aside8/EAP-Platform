package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class HsmsMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> out) throws Exception {
        HsmsMessage hsmsMessage = new HsmsMessage();
        hsmsMessage.decode(byteBuf);
        out.add(hsmsMessage);
    }
}
