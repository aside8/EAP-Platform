package com.github.aside8.eap.protocol.secs2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SecsFuzzTest {
    // Defaults: can be overridden with -Dsecs.fuzz.iterations=<n> and -Dfuzz.seed=<n>
    private static final int ITERS = Integer.getInteger("secs.fuzz.iterations", 2000);
    private static final long SEED = Long.getLong("fuzz.seed", System.nanoTime());
    private static final Random R = new Random(SEED);

    @Test
    @Timeout(value = 30)
    void seedIsPrintableForRepro() {
        System.err.println("SecsFuzzTest seed=" + SEED + " iterations=" + ITERS);
        assertTrue(ITERS > 0);
    }

    @Test
    @Timeout(value = 120)
    void corpusEdgeCases() {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;

        // Minimal legal: zero-length ASCII
        SecsDataItem zeroAscii = SecsDataItem.ascii("", StandardCharsets.US_ASCII);
        SecsDataItem decoded = roundTrip(alloc, zeroAscii);
        assertEquals(zeroAscii.getAscii(), decoded.getAscii());

        // Minimal list
        SecsDataItem emptyList = SecsDataItem.list();
        SecsDataItem decodedList = roundTrip(alloc, emptyList);
        assertEquals(0, decodedList.getList().size());

        // Truncated data (declare length > available)
        byte[] truncated = { (byte) (SecsFormatCode.BINARY.getValue() | 0x01), 0x05, 0x01, 0x02 };
        final ByteBuf tb = Unpooled.wrappedBuffer(truncated);
        try {
            SecsDataItem s = new SecsDataItem();
            Assertions.assertThrows(IllegalArgumentException.class, () -> s.decode(tb));
        } finally {
            ReferenceCountUtil.release(tb);
        }

        // Fixed-size misalignment (length not multiple of element size)
        byte[] badLen = { (byte) (SecsFormatCode.INT2.getValue() | 0x01), 0x01, 0x00 };
        final ByteBuf mb = Unpooled.wrappedBuffer(badLen);
        try {
            SecsDataItem s = new SecsDataItem();
            Assertions.assertThrows(IllegalArgumentException.class, () -> s.decode(mb));
        } finally {
            ReferenceCountUtil.release(mb);
        }
    }

    @Test
    @Timeout(value = 180)
    void randomDecodeDoesNotThrowUnexpectedExceptions() {
        ByteBufAllocator alloc = ByteBufAllocator.DEFAULT;
        for (int i = 0; i < ITERS; i++) {
            int len = R.nextInt(512);
            byte[] bytes = new byte[len];
            R.nextBytes(bytes);
            ByteBuf in = Unpooled.wrappedBuffer(bytes);
            try {
                SecsDataItem s = new SecsDataItem();
                try {
                    s.decode(in);
                    // decoded or partially decoded -> fine
                } catch (IllegalArgumentException expected) {
                    // allowed: malformed inputs should surface as IllegalArgumentException
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
            SecsDataItem item = randomValidItem(R, 0);
            /* validate by comparing encode->decode->encode bytes (avoids relying on helper getters) */
            ByteBuf encoded = null;
            ByteBuf reencoded = null;
            try {
                encoded = item.encode(alloc);
                byte[] a = new byte[encoded.readableBytes()];
                encoded.getBytes(encoded.readerIndex(), a);

                SecsDataItem decoded = roundTrip(alloc, item);

                reencoded = decoded.encode(alloc);
                byte[] b = new byte[reencoded.readableBytes()];
                reencoded.getBytes(reencoded.readerIndex(), b);

                assertArrayEquals(a, b, "round-trip bytes mismatch (seed=" + SEED + ", iter=" + i + ")");
            } finally {
                if (encoded != null) ReferenceCountUtil.release(encoded);
                if (reencoded != null) ReferenceCountUtil.release(reencoded);
            }
        }
    }

    /* ---------- helpers ---------- */
    private static SecsDataItem roundTrip(ByteBufAllocator alloc, SecsDataItem item) {
        ByteBuf buf = null;
        try {
            buf = item.encode(alloc);
            // copy to a wrapped buffer so we can release safely
            byte[] bytes = new byte[buf.readableBytes()];
            buf.getBytes(buf.readerIndex(), bytes);
            ByteBuf in = Unpooled.wrappedBuffer(bytes);
            try {
                SecsDataItem decoded = new SecsDataItem();
                decoded.decode(in);
                return decoded;
            } finally {
                ReferenceCountUtil.release(in);
            }
        } finally {
            if (buf != null) ReferenceCountUtil.release(buf);
        }
    }

    private static SecsDataItem randomValidItem(Random r, int depth) {
        SecsFormatCode[] formats = SecsFormatCode.values();
        SecsFormatCode fmt = formats[r.nextInt(formats.length)];

        // Bias towards small sizes to keep tests fast
        int cardinality = r.nextInt(5);
        switch (fmt) {
            case LIST -> {
                int size = r.nextInt(1 + cardinality);
                List<SECSII> items = new ArrayList<>();
                if (depth < 3) {
                    for (int i = 0; i < size; i++) items.add(randomValidItem(r, depth + 1));
                }
                return SecsDataItem.list(items.toArray(new SECSII[0]));
            }
            case ASCII -> {
                int l = r.nextInt(1 + cardinality * 4);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < l; i++) sb.append((char) (32 + r.nextInt(95)));
                return SecsDataItem.ascii(sb.toString(), StandardCharsets.US_ASCII);
            }
            case BINARY -> {
                byte[] b = new byte[cardinality];
                r.nextBytes(b);
                return SecsDataItem.binary(b);
            }
            case BOOLEAN -> {
                boolean[] b = new boolean[cardinality];
                for (int i = 0; i < b.length; i++) b[i] = r.nextBoolean();
                return SecsDataItem.bool(b);
            }
            case INT1 -> {
                byte[] bb = new byte[cardinality];
                r.nextBytes(bb);
                return SecsDataItem.int1(bb);
            }
            case INT2 -> {
                short[] arr = new short[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = (short) r.nextInt();
                return SecsDataItem.int2(arr);
            }
            case INT4 -> {
                int[] arr = new int[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextInt();
                return SecsDataItem.int4(arr);
            }
            case INT8 -> {
                long[] arr = new long[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextLong();
                return SecsDataItem.int8(arr);
            }
            case UINT1 -> {
                short[] arr = new short[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = (short) (r.nextInt(0x100));
                return SecsDataItem.uint1(arr);
            }
            case UINT2 -> {
                int[] arr = new int[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextInt(0x10000);
                return SecsDataItem.uint2(arr);
            }
            case UINT4 -> {
                long[] arr = new long[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextInt() & 0xFFFFFFFFL;
                return SecsDataItem.uint4(arr);
            }
            case UINT8 -> {
                long[] arr = new long[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextLong();
                return SecsDataItem.uint8(arr);
            }
            case FLOAT4 -> {
                float[] arr = new float[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextFloat();
                return SecsDataItem.float4(arr);
            }
            case FLOAT8 -> {
                double[] arr = new double[cardinality];
                for (int i = 0; i < arr.length; i++) arr[i] = r.nextDouble();
                return SecsDataItem.float8(arr);
            }
            default -> {
                // fallback to binary
                byte[] b = new byte[cardinality];
                r.nextBytes(b);
                return SecsDataItem.binary(b);
            }
        }
    }

    private static String hexdump(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, 128); i++) {
            sb.append(String.format("%02X", b[i]));
        }
        if (b.length > 128) sb.append("...");
        return sb.toString();
    }
}
