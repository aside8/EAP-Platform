package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;

public class FixedLength4ByteDecoder extends FixedLengthFrameDecoder {

    public FixedLength4ByteDecoder() {
        super(4);
    }
}
