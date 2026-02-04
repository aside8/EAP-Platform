package com.github.aside8.eap.protocol.secs2;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
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
    void uint8_roundtrip_and_access() {
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        SecsDataItem u8 = SecsDataItem.uint8(1L, 2L, 3L);
        long[] vals = u8.getUint8();
        assertArrayEquals(new long[]{1L, 2L, 3L}, vals);

        ByteBuf enc = u8.encode(allocator);
        try {
            SecsDataItem decoded = new SecsDataItem();
            decoded.decode(enc);
            assertArrayEquals(u8.getUint8(), decoded.getUint8());
        } finally {
            enc.release();
        }
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

    @Test
    void decode() {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        String buffer = "0000860b00000000000a0103b10400000000b10400007d0001010102b10400007d000103411032303235303630333136343030323137a50101410731324231313131";
        for (int i = 0; i < buffer.length(); i += 2) {
            int b = Integer.parseInt(buffer.substring(i, i + 2), 16);
            byteBuf.writeByte((byte)b);
        }
        HsmsMessage hsmsMessage  = new HsmsMessage();
        hsmsMessage.decode(byteBuf);
        assertEquals(6, hsmsMessage.getStream());
        assertEquals(11, hsmsMessage.getFunction());

        SECSII secs2 = hsmsMessage.getBody();
        assertEquals(0L, secs2.get(0).getUint4(0));
        assertEquals(32000L, secs2.get(1).getUint4(0));
        SECSII properties = secs2.get(2).get(0);

        assertEquals(32000, properties.get(0).getUint4(0));
        assertEquals("2025060316400217", properties.get(1).get(0).getAscii());
        assertEquals((short) 1, properties.get(1).get(1).getUint1(0));
        assertEquals("12B1111", properties.get(1).get(2).getAscii());
        System.out.println(hsmsMessage.getBody().toString());
    }

    @Test
    void zeroLengthItem_usesMinimalLengthEncoding_and_roundtrips() {
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        SecsDataItem emptyBin = SecsDataItem.binary(new byte[0]);
        ByteBuf encoded = emptyBin.encode(allocator);
        // format byte present and low two bits == 0 (minimal encoding)
        byte format = encoded.getByte(0);
        assertEquals(0, format & 0x03);

        SecsDataItem decoded = new SecsDataItem();
        decoded.decode(encoded);
        assertArrayEquals(new byte[0], decoded.getBinary());

        // also test empty LIST minimal encoding
        SecsDataItem emptyList = SecsDataItem.list();
        ByteBuf listEncoded = emptyList.encode(allocator);
        assertEquals(0, listEncoded.getByte(0) & 0x03);
        SecsDataItem decodedList = new SecsDataItem();
        decodedList.decode(listEncoded);
        assertEquals(0, decodedList.getList().size());
    }

    @Test
    void decode_truncatedData_throws() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        // ASCII with length=4 but only 2 bytes provided
        buf.writeByte((byte) (SecsFormatCode.ASCII.getValue() | 1));
        buf.writeByte(4);
        buf.writeByte('A');
        buf.writeByte('B');
        SecsDataItem item = new SecsDataItem();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.decode(buf));
        assertTrue(ex.getMessage().contains("Truncated"));
    }

    @Test
    void decode_fixedSizeLength_mustBeMultipleOfElementSize() {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        // I4 with length=3 (invalid)
        buf.writeByte((byte) (SecsFormatCode.INT4.getValue() | 1));
        buf.writeByte(3);
        buf.writeByte(0);
        buf.writeByte(1);
        SecsDataItem item = new SecsDataItem();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> item.decode(buf));
        assertTrue(ex.getMessage().contains("not a multiple of element size"));
    }
}