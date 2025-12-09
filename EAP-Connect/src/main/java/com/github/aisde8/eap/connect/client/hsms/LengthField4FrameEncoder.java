package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldPrepender;

public class LengthField4FrameEncoder extends LengthFieldPrepender {

    public LengthField4FrameEncoder() {
        super(4, 0);
    }
}
