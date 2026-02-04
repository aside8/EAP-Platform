package com.github.aisde8.eap.connect.server.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HsmsServerFramingTest {

    @Test
    void server_pipeline_respects_includeLength_setting() {
        HsmsMessage msg = HsmsMessages.dataReq(1, true, 6, 11, 0xCAFEBABE, SecsDataItem.binary((byte) 0x01));

        for (boolean includeLength : new boolean[]{false, true}) {
            // server pipeline: LengthField4FrameDecoder -> HsmsMessageDecoder -> HsmsMessageEncoder -> LengthField4FrameEncoder
            EmbeddedChannel ch = new EmbeddedChannel(
                    new com.github.aisde8.eap.connect.client.hsms.LengthField4FrameEncoder(includeLength),
                    new com.github.aisde8.eap.connect.client.hsms.HsmsMessageEncoder(),
                    new com.github.aisde8.eap.connect.client.hsms.LengthField4FrameDecoder(includeLength)
            );

            ByteBuf payload = msg.encode(ByteBufAllocator.DEFAULT);
            try {
                // emulate server encoding then decoding (roundtrip)
                boolean wrote = ch.writeOutbound(payload.retain());
                assertTrue(wrote || ch.outboundMessages().size() > 0, "server encoder produced no outbound parts for includeLength=" + includeLength);

                // read and coalesce outbound parts
                io.netty.buffer.CompositeByteBuf composite = ByteBufAllocator.DEFAULT.compositeBuffer();
                ByteBuf part;
                while ((part = ch.readOutbound()) != null) {
                    composite.addComponent(true, part);
                }

                assertTrue(composite.isReadable(), "framed composite must be readable");

                boolean decoded = ch.writeInbound(composite.retain());
                assertTrue(decoded, "server decoder failed to accept framed bytes for includeLength=" + includeLength + ", first4=0x" + Integer.toHexString(composite.getInt(0)));
                ByteBuf decodedPayload = ch.readInbound();
                assertNotNull(decodedPayload);

                HsmsMessage parsed = new HsmsMessage();
                parsed.decode(decodedPayload);

                assertEquals(msg.getStream(), parsed.getStream());
                assertEquals(msg.getFunction(), parsed.getFunction());
                assertEquals(msg.getSystemBytes(), parsed.getSystemBytes());
            } finally {
                payload.release();
                ch.finishAndReleaseAll();
            }
        }
    }
}
