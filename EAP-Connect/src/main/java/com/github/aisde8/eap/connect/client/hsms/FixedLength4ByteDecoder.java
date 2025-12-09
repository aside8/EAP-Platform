package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class FixedLength4ByteDecoder extends LengthFieldBasedFrameDecoder {

    public FixedLength4ByteDecoder() {
        super(1024 * 1024, 0, 4, 0, 4);
    }
}
