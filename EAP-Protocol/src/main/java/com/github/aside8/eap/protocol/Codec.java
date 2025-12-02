package com.github.aside8.eap.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * An interface for objects that can be encoded to and decoded from a {@link ByteBuf}.
 */
public interface Codec {

    /**
     * Encodes this object into a {@link ByteBuf}.
     *
     * @param allocator a {@link ByteBufAllocator} which will be used to allocate the returned {@link ByteBuf}.
     * @return a {@link ByteBuf} containing the encoded object.
     */
    ByteBuf encode(ByteBufAllocator allocator);

    /**
     * Decodes this object from a {@link ByteBuf}.
     *
     * @param in the {@link ByteBuf} to decode from.
     */
    void decode(ByteBuf in);
}
