package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 4‑byte length field decoder with configurable "length includes length field" behavior.
 *
 * Default behaviour is unchanged (length does NOT include the 4 bytes) to preserve backwards compatibility.
 */
public class LengthField4FrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final int MAX_FRAME_LENGTH = 4 * 1024 * 1024 + 4;

    /**
     * Backwards-compatible default: length field does NOT include the length of the length field itself.
     */
    public LengthField4FrameDecoder() {
        this(false);
    }

    /**
     * @param lengthIncludesLength true if the 4-byte length field includes the length of the length field
     */
    public LengthField4FrameDecoder(boolean lengthIncludesLength) {
        // If the length field includes the 4 bytes, the actual payload length = value - 4.
        super(MAX_FRAME_LENGTH, 0, 4, lengthIncludesLength ? -4 : 0, 4);
    }
}
