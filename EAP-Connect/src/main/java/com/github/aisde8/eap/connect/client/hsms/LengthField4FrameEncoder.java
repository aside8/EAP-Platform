package com.github.aisde8.eap.connect.client.hsms;

import io.netty.handler.codec.LengthFieldPrepender;

/**
 * 4‑byte length prepend encoder with configurable "length includes length field" behavior.
 *
 * Default behaviour is unchanged (length does NOT include the 4 bytes) to preserve backwards compatibility.
 */
public class LengthField4FrameEncoder extends LengthFieldPrepender {

    /**
     * Backwards-compatible default: length field does NOT include the length of the length field.
     */
    public LengthField4FrameEncoder() {
        this(false);
    }

    /**
     * @param lengthIncludesLength true if the length field should include the length of the length field itself
     */
    public LengthField4FrameEncoder(boolean lengthIncludesLength) {
        super(4, lengthIncludesLength);
    }
}
