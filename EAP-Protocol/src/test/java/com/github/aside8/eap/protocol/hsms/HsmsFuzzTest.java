package com.github.aside8.eap.protocol.hsms;

import com.github.aside8.eap.protocol.secs2.SecsDataItem;
import com.github.aside8.eap.protocol.secs2.sml.SmlPrinter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class HsmsFuzzTest {
    private static final int ITERS = Integer.getInteger("hsms.fuzz.iterations", 2000);
    private static final long SEED = Long.getLong("fuzz.seed", System.nanoTime());
    private static final Random R = new Random(SEED);

    @Test
    @Timeout(value = 30)
    void seedIsPrintableForRepro() {
        System.err.println("HsmsFuzzTest seed=" + SEED + " iterations=" + ITERS);
        assertTrue(ITERS > 0);
    }

    @Test
    @Timeout(value = 120)
    void corpusEdgeCases() {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;

        // empty buffer
        ByteBuf b = Unpooled.EMPTY_BUFFER;
        try {
            HsmsMessage m = new HsmsMessage();
            assertThrows(IllegalArgumentException.class, () -> m.decode(b));
        } finally {
            ReferenceCountUtil.release(b);
        }

        // header says there's body but buffer truncated
        HsmsMessage req = HsmsMessages.selectReq(1, 0x1234);
        ByteBuf encoded = req.encode(alloc);
        try {
            byte[] bytes = new byte[encoded.readableBytes() - 2]; // truncate
            encoded.getBytes(encoded.readerIndex(), bytes);
            ByteBuf in = Unpooled.wrappedBuffer(bytes);
            try {
                HsmsMessage m = new HsmsMessage();
                assertThrows(IllegalArgumentException.class, () -> m.decode(in));
            } finally {
                ReferenceCountUtil.release(in);
            }
        } finally {
            ReferenceCountUtil.release(encoded);
        }
    }

    @Test
    @Timeout(value = 180)
    void randomDecodeDoesNotThrowUnexpectedExceptions() {
        for (int i = 0; i < ITERS; i++) {
            int len = R.nextInt(1024);
            byte[] bytes = new byte[len];
            R.nextBytes(bytes);
            ByteBuf in = Unpooled.wrappedBuffer(bytes);
            try {
                HsmsMessage m = new HsmsMessage();
                try {
                    m.decode(in);
                } catch (IllegalArgumentException expected) {
                    // allowed for malformed input
                } catch (Throwable t) {
                    fail(String.format("Unexpected exception (seed=%d, iter=%d): %s\nbytes=%s",
                            SEED, i, t.getClass().getName(), hexdump(bytes)));
                }
            } finally {
                ReferenceCountUtil.release(in);
            }
        }
    }

    @Test
    @Timeout(value = 180)
    void randomValidRoundTrip() {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;
        for (int i = 0; i < ITERS; i++) {
            HsmsMessage m = randomValidMessage(R);
            ByteBuf buf = null;
            try {
                buf = m.encode(alloc);
                byte[] bytes = new byte[buf.readableBytes()];
                buf.getBytes(buf.readerIndex(), bytes);
                ByteBuf in = Unpooled.wrappedBuffer(bytes);
                try {
                    HsmsMessage decoded = new HsmsMessage();
                    decoded.decode(in);
                    /* compare public API: stream/function/systemBytes */
                    assertEquals(m.getStream(), decoded.getStream());
                    assertEquals(m.getFunction(), decoded.getFunction());
                    assertEquals(m.getSystemBytes(), decoded.getSystemBytes());
                    if (m.getBody() != null) {
                        assertNotNull(decoded.getBody());
                        /* SECSII doesn't expose toFormatString; cast to SecsDataItem for structural comparison */
                        assertEquals(SmlPrinter.DEFAULT.toSml((SecsDataItem) m.getBody()), SmlPrinter.DEFAULT.toSml((SecsDataItem) decoded.getBody()));
                    } else {
                        assertNull(decoded.getBody());
                    }
                } finally {
                    ReferenceCountUtil.release(in);
                }
            } finally {
                if (buf != null) ReferenceCountUtil.release(buf);
            }
        }
    }

    /* ---------- helpers ---------- */
    private static HsmsMessage randomValidMessage(Random r) {
        int choice = r.nextInt(5);
        int deviceId = 1 + r.nextInt(15);
        int system = r.nextInt();
        return switch (choice) {
            case 0 -> HsmsMessages.selectReq(deviceId, system);
            case 1 -> HsmsMessages.deselectReq(deviceId, system);
            case 2 -> HsmsMessages.linkTestReq(system);
            case 3 -> HsmsMessages.selectReq(deviceId, system);
            default -> HsmsMessages.dataReq(deviceId, r.nextBoolean(), 1 + r.nextInt(63), 1 + r.nextInt(63), system,
                    SecsDataItem.binary((byte) r.nextInt(0xFF)));
        };
    }

    private static String hexdump(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, 128); i++) sb.append(String.format("%02X", b[i]));
        if (b.length > 128) sb.append("...");
        return sb.toString();
    }
}
