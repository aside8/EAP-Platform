package com.github.aside8.eap.protocol.secs2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecsDataItemTest {

    @Test
    void encode() {
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
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

    @Test
    void testToString() {
        SecsDataItem nestedItem = SecsDataItem.list(
                SecsDataItem.ascii("2025060316400217"),
                SecsDataItem.uint1((short) 1),
                SecsDataItem.ascii("12B1111")
        );

        SecsDataItem item = SecsDataItem.list(
                SecsDataItem.uint4(0),
                SecsDataItem.uint4(32000),
                SecsDataItem.list(
                        SecsDataItem.list(
                                SecsDataItem.uint4(32000),
                                nestedItem
                        )
                )
        );

        String expected =
                "<L [3]\n" +
                        "  <U4 [1] 0>\n" +
                        "  <U4 [1] 32000>\n" +
                        "  <L [1]\n" +
                        "    <L [2]\n" +
                        "      <U4 [1] 32000>\n" +
                        "      <L [3]\n" +
                        "        <A [16] \"2025060316400217\">\n" +
                        "        <U1 [1] 1>\n" +
                        "        <A [7] \"12B1111\">\n" +
                        "      >\n" +
                        "    >\n" +
                        "  >\n" +
                        ">\n";

        String actual = item.toString();
        assertEquals(expected.replace("\r\n", "\n"), actual.replace("\r\n", "\n"));

        expected = "<B [1] 0x00>\n";
        item = SecsDataItem.binary((byte) 0);
        assertEquals(expected.replace("\r\n", "\n"), item.toString());
    }
}