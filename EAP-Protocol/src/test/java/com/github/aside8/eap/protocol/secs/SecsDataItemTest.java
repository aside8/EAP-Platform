package com.github.aside8.eap.protocol.secs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecsDataItemTest {

    private ByteBufAllocator allocator;

    @BeforeEach
    void setUp() {
        allocator = new UnpooledByteBufAllocator(false);
    }

    // --- Factory Methods Tests ---

    @Test
    void testOfList() {
        SecsDataItem item1 = SecsDataItem.ofAscii("TEST");
        SecsDataItem item2 = SecsDataItem.ofU4(1, 2, 3);
        SecsDataItem list = SecsDataItem.ofList(item1, item2);

        assertNotNull(list);
        assertEquals(SecsFormatCode.L, list.getFormatCode());
        assertEquals(2, list.getListItems().size());
        assertEquals(item1, list.getListItems().get(0));
        assertEquals(item2, list.getListItems().get(1));
    }

    @Test
    void testOfListEmpty() {
        SecsDataItem list = SecsDataItem.ofList();
        assertNotNull(list);
        assertEquals(SecsFormatCode.L, list.getFormatCode());
        assertTrue(list.getListItems().isEmpty());
    }

    @Test
    void testOfU4() {
        SecsDataItem item = SecsDataItem.ofU4(0L, 0xFFFFFFFFL);
        assertNotNull(item);
        assertEquals(SecsFormatCode.U4, item.getFormatCode());
        assertArrayEquals(new long[]{0L, 0xFFFFFFFFL}, item.getU4());
    }

    @Test
    void testOfU4Empty() {
        SecsDataItem item = SecsDataItem.ofU4();
        assertNotNull(item);
        assertEquals(SecsFormatCode.U4, item.getFormatCode());
        assertEquals(0, item.getU4().length);
    }

    @Test
    void testOfU4Invalid() {
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU4(-1L));
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU4(0xFFFFFFFFL + 1));
    }

    @Test
    void testOfBinary() {
        byte[] bytes = {1, 2, 3, 4};
        SecsDataItem item = SecsDataItem.ofBinary(bytes);
        assertNotNull(item);
        assertEquals(SecsFormatCode.B, item.getFormatCode());
        assertArrayEquals(bytes, item.getBinary());
    }

    @Test
    void testOfBoolean() {
        SecsDataItem item = SecsDataItem.ofBoolean(true, false, true);
        assertNotNull(item);
        assertEquals(SecsFormatCode.BOOLEAN, item.getFormatCode());
        assertArrayEquals(new boolean[]{true, false, true}, item.getBoolean());
    }

    @Test
    void testOfAsciiDefaultCharset() {
        String testString = "Hello, World!";
        SecsDataItem item = SecsDataItem.ofAscii(testString);
        assertNotNull(item);
        assertEquals(SecsFormatCode.A, item.getFormatCode());
        assertEquals(testString, item.getAscii());
        assertArrayEquals(testString.getBytes(StandardCharsets.US_ASCII), item.getRawBytes());
    }

    @Test
    void testOfAsciiSpecificCharset() {
        String testString = "你好，世界！";
        Charset charset = StandardCharsets.UTF_8;
        SecsDataItem item = SecsDataItem.ofAscii(testString, charset);
        assertNotNull(item);
        assertEquals(SecsFormatCode.A, item.getFormatCode());
        assertEquals(testString, item.getAscii(charset));
        assertArrayEquals(testString.getBytes(charset), item.getRawBytes());
    }
    
    @Test
    void testOfAsciiSpecificCharset_GBK() {
        String testString = "你好，世界！";
        Charset charset = Charset.forName("GBK");
        SecsDataItem item = SecsDataItem.ofAscii(testString, charset);
        assertNotNull(item);
        assertEquals(SecsFormatCode.A, item.getFormatCode());
        assertEquals(testString, item.getAscii(charset));
        assertArrayEquals(testString.getBytes(charset), item.getRawBytes());
    }

    @Test
    void testOfAsciiNull() {
        SecsDataItem item = SecsDataItem.ofAscii(null);
        assertNotNull(item);
        assertEquals(SecsFormatCode.A, item.getFormatCode());
        assertTrue(item.getAscii().isEmpty());
    }

    @Test
    void testOfI8() {
        SecsDataItem item = SecsDataItem.ofI8(Long.MIN_VALUE, 0L, Long.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.I8, item.getFormatCode());
        assertArrayEquals(new long[]{Long.MIN_VALUE, 0L, Long.MAX_VALUE}, item.getI8());
    }

    @Test
    void testOfU8() {
        SecsDataItem item = SecsDataItem.ofU8(0L, -1L); // -1L is 0xFFFFFFFFFFFFFFFFL unsigned
        assertNotNull(item);
        assertEquals(SecsFormatCode.U8, item.getFormatCode());
        assertArrayEquals(new long[]{0L, -1L}, item.getU8()); // Java long keeps sign
    }

    @Test
    void testOfF8() {
        SecsDataItem item = SecsDataItem.ofF8(1.23, -4.56, Double.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.F8, item.getFormatCode());
        assertArrayEquals(new double[]{1.23, -4.56, Double.MAX_VALUE}, item.getF8(), 0.0001);
    }

    @Test
    void testOfF4() {
        SecsDataItem item = SecsDataItem.ofF4(1.23f, -4.56f, Float.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.F4, item.getFormatCode());
        assertArrayEquals(new float[]{1.23f, -4.56f, Float.MAX_VALUE}, item.getF4(), 0.0001f);
    }

    @Test
    void testOfI4() {
        SecsDataItem item = SecsDataItem.ofI4(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.I4, item.getFormatCode());
        assertArrayEquals(new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE}, item.getI4());
    }

    @Test
    void testOfI2() {
        SecsDataItem item = SecsDataItem.ofI2(Short.MIN_VALUE, (short) 0, Short.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.I2, item.getFormatCode());
        assertArrayEquals(new short[]{Short.MIN_VALUE, (short) 0, Short.MAX_VALUE}, item.getI2());
    }

    @Test
    void testOfU2() {
        SecsDataItem item = SecsDataItem.ofU2(0, 0xFFFF);
        assertNotNull(item);
        assertEquals(SecsFormatCode.U2, item.getFormatCode());
        assertArrayEquals(new int[]{0, 0xFFFF}, item.getU2());
    }

    @Test
    void testOfU2Invalid() {
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU2(-1));
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU2(0xFFFF + 1));
    }

    @Test
    void testOfI1() {
        SecsDataItem item = SecsDataItem.ofI1(Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE);
        assertNotNull(item);
        assertEquals(SecsFormatCode.I1, item.getFormatCode());
        assertArrayEquals(new byte[]{Byte.MIN_VALUE, (byte) 0, Byte.MAX_VALUE}, item.getI1());
    }

    @Test
    void testOfU1() {
        SecsDataItem item = SecsDataItem.ofU1((short) 0, (short) 0xFF);
        assertNotNull(item);
        assertEquals(SecsFormatCode.U1, item.getFormatCode());
        assertArrayEquals(new short[]{0, 0xFF}, item.getU1());
    }

    @Test
    void testOfU1Invalid() {
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU1((short) -1));
        assertThrows(IllegalArgumentException.class, () -> SecsDataItem.ofU1((short) (0xFF + 1)));
    }


    // --- Encode/Decode Roundtrip Tests ---

    private SecsDataItem encodeAndDecode(SecsDataItem original) {
        ByteBuf encoded = original.encode(allocator);
        SecsDataItem decoded = new SecsDataItem();
        decoded.decode(encoded);
        encoded.release();
        return decoded;
    }

    @Test
    void testRoundtripList() {
        SecsDataItem nestedAscii = SecsDataItem.ofAscii("Nested");
        SecsDataItem nestedU4 = SecsDataItem.ofU4(100L);
        SecsDataItem original = SecsDataItem.ofList(nestedAscii, nestedU4);

        SecsDataItem decoded = encodeAndDecode(original);

        assertEquals(SecsFormatCode.L, decoded.getFormatCode());
        assertEquals(2, decoded.getListItems().size());
        assertEquals(SecsFormatCode.A, ((SecsDataItem) decoded.getListItems().get(0)).getFormatCode());
        assertEquals("Nested", ((SecsDataItem) decoded.getListItems().get(0)).getAscii());
        assertEquals(SecsFormatCode.U4, ((SecsDataItem) decoded.getListItems().get(1)).getFormatCode());
        assertArrayEquals(new long[]{100L}, ((SecsDataItem) decoded.getListItems().get(1)).getU4());
    }

    @Test
    void testRoundtripU4() {
        SecsDataItem original = SecsDataItem.ofU4(12345L, 67890L);
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.U4, decoded.getFormatCode());
        assertArrayEquals(original.getU4(), decoded.getU4());
    }

    @Test
    void testRoundtripAscii() {
        String testString = "Test ASCII with various characters: !@#$%^&*()_+-=[]{}\\|;:'\"<>,./?~`";
        SecsDataItem original = SecsDataItem.ofAscii(testString, StandardCharsets.UTF_8);
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.A, decoded.getFormatCode());
        assertEquals(testString, decoded.getAscii(StandardCharsets.UTF_8));
    }
    
    @Test
    void testRoundtripAscii_GBK() {
        String testString = "测试中文编码";
        Charset charset = Charset.forName("GBK");
        SecsDataItem original = SecsDataItem.ofAscii(testString, charset);
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.A, decoded.getFormatCode());
        assertEquals(testString, decoded.getAscii(charset));
    }

    @Test
    void testRoundtripEmptyList() {
        SecsDataItem original = SecsDataItem.ofList();
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.L, decoded.getFormatCode());
        assertTrue(decoded.getListItems().isEmpty());
    }

    @Test
    void testRoundtripEmptyAscii() {
        SecsDataItem original = SecsDataItem.ofAscii("");
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.A, decoded.getFormatCode());
        assertTrue(decoded.getAscii().isEmpty());
    }
    
    @Test
    void testRoundtripBinary() {
        byte[] bytes = { (byte)0xFF, 0x00, 0x12, (byte)0xCD };
        SecsDataItem original = SecsDataItem.ofBinary(bytes);
        SecsDataItem decoded = encodeAndDecode(original);
        assertEquals(SecsFormatCode.B, decoded.getFormatCode());
        assertArrayEquals(original.getBinary(), decoded.getBinary());
    }

    // --- Getter Type Safety / Exception Tests ---

    @Test
    void testGetAsciiOnWrongTypeThrowsException() {
        SecsDataItem item = SecsDataItem.ofU4(123L);
        assertThrows(IllegalStateException.class, item::getAscii);
    }

    @Test
    void testGetU4OnWrongTypeThrowsException() {
        SecsDataItem item = SecsDataItem.ofAscii("Hello");
        assertThrows(IllegalStateException.class, item::getU4);
    }
    
    @Test
    void testGetListItemsOnWrongTypeThrowsException() {
        SecsDataItem item = SecsDataItem.ofAscii("Hello");
        assertThrows(IllegalStateException.class, item::getListItems);
    }
    
    @Test
    void testGetRawBytesOnListTypeThrowsException() {
        SecsDataItem item = SecsDataItem.ofList(SecsDataItem.ofAscii("test"));
        assertThrows(IllegalStateException.class, item::getRawBytes);
    }
    
    @Test
    void testDecodeEmptyBuffer() {
        ByteBuf emptyBuffer = allocator.buffer(0);
        SecsDataItem item = new SecsDataItem();
        item.decode(emptyBuffer);
        assertEquals(SecsFormatCode.L, item.getFormatCode()); // Default initialized
        assertTrue(item.getListItems().isEmpty()); // Should reset to empty list
        assertArrayEquals(new byte[0], item.getRawBytes()); // internal value should be empty
        emptyBuffer.release();
    }
}
