package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class LengthField4FrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_LENGTH = 4 * 1024 * 1024 + 4;

    public LengthField4FrameDecoder() {
        super(MAX_FRAME_LENGTH, 0, 4, 0, 4);
    }
}
