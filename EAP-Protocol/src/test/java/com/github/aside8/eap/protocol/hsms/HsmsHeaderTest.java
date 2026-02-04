package com.github.aside8.eap.protocol.hsms;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HsmsHeaderTest {

    @Test
    void encodeDecode_roundtrip_preservesWbitAndStream() {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) 0x1234)
                .stream((byte) 0x7F) // max 7-bit value
                .function((byte) 0xAA)
                .wbit(true)
                .ptype((byte) 0x00)
                .stype((byte) 0x00)
                .systemBytes(0x01020304)
                .build();

        var encoded = header.encode(ByteBufAllocator.DEFAULT);
        try {
            HsmsHeader decoded = new HsmsHeader();
            decoded.decode(ByteBufAllocator.DEFAULT.buffer().writeBytes(encoded));

            assertEquals(header.isWbit(), decoded.isWbit());
            assertEquals(header.getStream(), decoded.getStream());
            assertEquals(header.getSystemBytes(), decoded.getSystemBytes());
        } finally {
            encoded.release();
        }
    }

    @Test
    void encode_invalidStream_throws() {
        HsmsHeader header = HsmsHeader.builder()
                .stream((byte) 0x80) // out of 7-bit range
                .build();
        assertThrows(IllegalArgumentException.class, () -> header.encode(ByteBufAllocator.DEFAULT));
    }

    @Test
    void decode_shortBuffer_throws() {
        HsmsHeader header = new HsmsHeader();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> header.decode(ByteBufAllocator.DEFAULT.buffer(5)));
        assertTrue(ex.getMessage().contains("Header must be 10 bytes"));
    }
}
