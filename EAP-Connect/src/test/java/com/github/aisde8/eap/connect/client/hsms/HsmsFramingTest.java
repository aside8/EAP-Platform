package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HsmsFramingTest {

    @Test
    void framing_roundtrip_for_both_includeLength_modes() {
        HsmsMessage msg = HsmsMessages.dataReq(1, true, 6, 11, 0xCAFEBABE, SecsDataItem.binary((byte) 0x01));

        for (boolean includeLength : new boolean[]{false, true}) {
            // encode the HSMS message to a raw payload first (isolates Message->ByteBuf step)
            ByteBuf payload = msg.encode(ByteBufAllocator.DEFAULT);
            EmbeddedChannel lengthEncoder = new EmbeddedChannel(new LengthField4FrameEncoder(includeLength));
            try {
                boolean wrote = lengthEncoder.writeOutbound(payload.retain());

                // LengthFieldPrepender may emit multiple outbound parts (length header + payload); coalesce.
                io.netty.buffer.CompositeByteBuf framed = ByteBufAllocator.DEFAULT.compositeBuffer();
                ByteBuf part;
                while ((part = lengthEncoder.readOutbound()) != null) {
                    framed.addComponent(true, part);
                }

                assertTrue(framed.isReadable(), "framed must be readable for includeLength=" + includeLength + " (wrote=" + wrote + ")");

                // decode via pipeline configured with the same mode
                EmbeddedChannel decoder = new EmbeddedChannel(new LengthField4FrameDecoder(includeLength));
                try {
                    boolean decodedWrote = decoder.writeInbound(framed.retain());
                    if (!decodedWrote) {
                        fail("decoder.writeInbound returned false for includeLength=" + includeLength + "; framed.readableBytes=" + framed.readableBytes() + "; first4=0x" + Integer.toHexString(framed.getInt(0)));
                    }
                    ByteBuf decoded = decoder.readInbound();
                    assertNotNull(decoded);

                    HsmsMessage parsed = new HsmsMessage();
                    parsed.decode(decoded);

                    assertEquals(msg.getStream(), parsed.getStream(), "stream should match (includeLength=" + includeLength + ")");
                    assertEquals(msg.getFunction(), parsed.getFunction(), "function should match (includeLength=" + includeLength + ")");
                    assertEquals(msg.getSystemBytes(), parsed.getSystemBytes(), "systemBytes should match (includeLength=" + includeLength + ")");
                } finally {
                    decoder.finishAndReleaseAll();
                }
            } finally {
                payload.release();
                lengthEncoder.finishAndReleaseAll();
            }
        }
    }
}
