package com.github.aisde8.eap.connect.client.hsms;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FixedLength4ByteEncoder extends MessageToByteEncoder<ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, ByteBuf byteBuf2) throws Exception {
        byteBuf2.writeInt(byteBuf.readableBytes());
        byteBuf2.writeBytes(byteBuf);
    }
}
