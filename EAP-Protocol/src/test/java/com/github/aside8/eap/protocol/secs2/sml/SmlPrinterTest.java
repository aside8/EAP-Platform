package com.github.aside8.eap.protocol.secs2.sml;

import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import com.github.aside8.eap.protocol.secs2.SECSII;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmlPrinterTest {

    @Test
    void printerProducesExpectedFormat_forNestedItem() {
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

        String printed = SmlPrinter.DEFAULT.toSml(item);
        assertTrue(printed.contains("<L [3]"));
        assertTrue(printed.contains("<A [16] \"2025060316400217\""));

        // round-trip via parser using compact/machine SML
        String compact = SmlPrinter.DEFAULT.toMachineSml(item);
        SECSII parsed = SmlParser.parseItem(compact);
        SecsDataItem decoded = (SecsDataItem) parsed;
        SecsDataItem re = new SecsDataItem();
        re.decode(item.encode(ByteBufAllocator.DEFAULT));
        assertEquals(SmlPrinter.DEFAULT.toSml(re), SmlPrinter.DEFAULT.toSml(decoded));
    }

    @Test
    void printerHandlesBinaryAndNumbers() {
        SecsDataItem b = SecsDataItem.binary((byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE);
        String s = SmlPrinter.DEFAULT.toSml(b);
        assertTrue(s.contains("0xCA"));

        SecsDataItem u8 = SecsDataItem.uint8(1L, 2L, 3L);
        String su8 = SmlPrinter.DEFAULT.toSml(u8);
        assertTrue(su8.contains("U8"));
    }
}
