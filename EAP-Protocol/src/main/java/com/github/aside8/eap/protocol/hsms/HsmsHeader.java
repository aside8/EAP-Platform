package com.github.aside8.eap.protocol.hsms;

import com.github.aside8.eap.protocol.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HsmsHeader implements Codec {
    // HEADER SIZE fixed to 10 bytes
    private static final int HEADER_SIZE_BYTES = 10;

    private static final byte WBIT_MASK = (byte) 0b10000000;
    private static final byte STREAM_MASK = (byte) 0b01111111;

    private short sessionId; // a.k.a. Device ID
    private byte stream;
    private byte function;
    private boolean wbit;
    private byte ptype;
    private byte stype;
    private int systemBytes;

    @Override
    public ByteBuf encode(ByteBufAllocator allocator) {
        ByteBuf buf = allocator.buffer(HEADER_SIZE_BYTES);
        buf.writeShort(sessionId);
        buf.writeByte(stream);
        buf.writeByte(function);
        buf.writeByte(ptype);
        buf.writeByte(stype);
        buf.writeInt(systemBytes);
        // set W-Bit
        if (wbit) {
            buf.setByte(2, buf.getByte(2) | WBIT_MASK);
        }
        return buf;
    }

    @Override
    public void decode(ByteBuf in) {
        if (in.readableBytes() < HEADER_SIZE_BYTES) {
            throw new IllegalArgumentException("Header must be 10 bytes");
        }
        this.sessionId = in.readShort();
        byte streamAndWbit = in.readByte();
        this.wbit = (streamAndWbit & WBIT_MASK) != 0;
        this.stream = (byte) (streamAndWbit & STREAM_MASK);
        this.function = in.readByte();
        this.ptype = in.readByte();
        this.stype = in.readByte();
        this.systemBytes = in.readInt();
    }
}