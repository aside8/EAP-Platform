package com.github.aside8.eap.protocol.secs2.sml;

import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import com.github.aside8.eap.protocol.secs2.SECSII;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmlParserTest {

    @Test
    void parseAsciiAndBinary() {
        SECSII a = SmlParser.parseItem("A[\"OK\"]");
        assertEquals("OK", ((SecsDataItem) a).getAscii());

        SECSII b = SmlParser.parseItem("B[0xDE 0xAD 0xBE 0xEF]");
        assertArrayEquals(new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF}, ((SecsDataItem) b).getBinary());
    }

    @Test
    void parseNumericTypesAndRoundTrip() {
        SECSII u4 = SmlParser.parseItem("U4[0, 32000]");
        io.netty.buffer.ByteBuf buf4 = ((SecsDataItem) u4).encode(ByteBufAllocator.DEFAULT);
        try {
            SECSII decoded = new SecsDataItem();
            ((SecsDataItem) decoded).decode(buf4);
            assertArrayEquals(((SecsDataItem) u4).getUint4(), ((SecsDataItem) decoded).getUint4());
        } finally {
            buf4.release();
        }

        SECSII u8 = SmlParser.parseItem("U8[1,2,3]");
        io.netty.buffer.ByteBuf buf8 = ((SecsDataItem) u8).encode(ByteBufAllocator.DEFAULT);
        try {
            SECSII round = new SecsDataItem();
            ((SecsDataItem) round).decode(buf8);
            assertArrayEquals(((SecsDataItem) u8).getUint8(), ((SecsDataItem) round).getUint8());
        } finally {
            buf8.release();
        }
    }

    @Test
    void parseUnsignedHexValues() {
        SECSII u1 = SmlParser.parseItem("U1[0xFF]");
        assertEquals((short) 0xFF, ((SecsDataItem) u1).getUint1(0));

        SECSII u2 = SmlParser.parseItem("U2[0xFFFF]");
        assertEquals(0xFFFF, ((SecsDataItem) u2).getUint2()[0]);
    }

    @Test
    void parseNestedList_roundTrip() {
        String sml = "L[ U4[0], U4[32000], L[ L[ U4[32000], L[ A[\"2025060316400217\"], U1[1], A[\"12B1111\"] ] ] ] ]";
        SECSII item = SmlParser.parseItem(sml);

        SecsDataItem encoded = (SecsDataItem) item;
        SecsDataItem decoded = new SecsDataItem();
        decoded.decode(encoded.encode(ByteBufAllocator.DEFAULT));
        assertEquals(SmlPrinter.DEFAULT.toSml(encoded), SmlPrinter.DEFAULT.toSml(decoded));
    }

    @Test
    void errorsAreReportedWithPosition() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SmlParser.parseItem("A[\"no end"));
        assertTrue(ex.getMessage().contains("line="));
    }

    @Test
    void limitsPreventDeepNesting() {
        SmlOptions opts = new SmlOptions(2, 1000, 1024);
        StringBuilder sb = new StringBuilder();
        sb.append("L[");
        sb.append("L[");
        sb.append("L[");
        sb.append("U1[1]");
        sb.append("]");
        sb.append("]");
        sb.append("]");
        String sml = sb.toString();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SmlParser.parseItem(sml, opts));
        assertTrue(ex.getMessage().toLowerCase().contains("depth"));
    }
}
