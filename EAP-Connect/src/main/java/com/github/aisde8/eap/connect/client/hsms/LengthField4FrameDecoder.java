package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class LengthField4FrameDecoder extends LengthFieldBasedFrameDecoder {

    public LengthField4FrameDecoder() {
        super(4 * 1024 * 1024 + 4, 0, 4, 0, 4);
    }
}
