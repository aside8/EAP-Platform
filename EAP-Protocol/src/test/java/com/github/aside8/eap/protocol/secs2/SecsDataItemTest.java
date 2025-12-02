package com.github.aside8.eap.protocol.secs2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecsDataItemTest {

    @Test
    void encode() {
        ByteBufAllocator  allocator = ByteBufAllocator.DEFAULT;
        SecsDataItem dataItem = SecsDataItem.int8(1L, 2L, 3L);
        ByteBuf encoded = dataItem.encode(allocator);
        assertEquals(26, encoded.readableBytes());

        assertThrowsExactly(IllegalStateException.class, dataItem::getUint8);

        long[] i8 = dataItem.getInt8();
        assertEquals(3, i8.length);
        assertEquals(1L, i8[0]);
        assertEquals(2L, i8[1]);
        assertEquals(3L, i8[2]);

        SecsDataItem compare = new SecsDataItem();
        compare.decode(encoded);
        assertArrayEquals(dataItem.getInt8(), compare.getInt8());
    }
}