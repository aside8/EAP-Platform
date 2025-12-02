package com.github.aside8.eap.protocol.secs2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SecsDataItem implements SECSII {
    private SecsFormatCode formatCode;
    private byte[] value;
    private List<SECSII> listItems;

    public SecsDataItem() {
        this.formatCode = SecsFormatCode.L; // Default to List, will be updated during decode
        this.listItems = Collections.emptyList();
        this.value = new byte[0];
    }

    // Private constructors for different data types
    private SecsDataItem(SecsFormatCode formatCode, byte[] value, List<SECSII> listItems) {
        this.formatCode = formatCode;
        this.value = value;
        this.listItems = listItems;
    }

    // Static factory methods
    public static SecsDataItem ofList(SECSII... items) {
        List<SECSII> itemList = (items != null) ? Arrays.asList(items) : Collections.emptyList();
        return new SecsDataItem(SecsFormatCode.L, null, itemList);
    }

    public static SecsDataItem ofU4(long... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.U4, new byte[0], null);
        }
        for (long value : values) {
            if (value < 0 || value > 0xFFFFFFFFL) {
                throw new IllegalArgumentException("U4 values must be between 0 and 0xFFFFFFFFL");
            }
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4);
        for (long value : values) {
            bb.putInt((int) value);
        }
        return new SecsDataItem(SecsFormatCode.U4, bb.array(), null);
    }

    public static SecsDataItem ofBinary(byte... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.B, new byte[0], null);
        }
        return new SecsDataItem(SecsFormatCode.B, values, null);
    }

    public static SecsDataItem ofBoolean(boolean... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.BOOLEAN, new byte[0], null);
        }
        byte[] byteValues = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            byteValues[i] = (byte) (values[i] ? 1 : 0);
        }
        return new SecsDataItem(SecsFormatCode.BOOLEAN, byteValues, null);
    }

    public static SecsDataItem ofAscii(String value, Charset charset) {
        if (value == null) {
            return new SecsDataItem(SecsFormatCode.A, new byte[0], null);
        }
        return new SecsDataItem(SecsFormatCode.A, value.getBytes(charset), null);
    }
    
    public static SecsDataItem ofAscii(String value) {
        // Per SECS standard, 'A' format is US-ASCII.
        return ofAscii(value, StandardCharsets.US_ASCII);
    }

    public static SecsDataItem ofI8(long... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.I8, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
        for (long value : values) {
            bb.putLong(value);
        }
        return new SecsDataItem(SecsFormatCode.I8, bb.array(), null);
    }

    public static SecsDataItem ofU8(long... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.U8, new byte[0], null);
        }
        // ByteBuffer handles 8-byte longs directly
        ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
        for (long value : values) {
            bb.putLong(value);
        }
        return new SecsDataItem(SecsFormatCode.U8, bb.array(), null);
    }

    public static SecsDataItem ofF8(double... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.F8, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
        for (double value : values) {
            bb.putDouble(value);
        }
        return new SecsDataItem(SecsFormatCode.F8, bb.array(), null);
    }

    public static SecsDataItem ofF4(float... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.F4, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4);
        for (float value : values) {
            bb.putFloat(value);
        }
        return new SecsDataItem(SecsFormatCode.F4, bb.array(), null);
    }

    public static SecsDataItem ofI4(int... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.I4, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 4);
        for (int value : values) {
            bb.putInt(value);
        }
        return new SecsDataItem(SecsFormatCode.I4, bb.array(), null);
    }

    public static SecsDataItem ofI2(short... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.I2, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 2);
        for (short value : values) {
            bb.putShort(value);
        }
        return new SecsDataItem(SecsFormatCode.I2, bb.array(), null);
    }

    public static SecsDataItem ofU2(int... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.U2, new byte[0], null);
        }
        for (int value : values) {
            if (value < 0 || value > 0xFFFF) {
                throw new IllegalArgumentException("U2 values must be between 0 and 0xFFFF");
            }
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * 2);
        for (int value : values) {
            bb.putShort((short) value);
        }
        return new SecsDataItem(SecsFormatCode.U2, bb.array(), null);
    }

    public static SecsDataItem ofI1(byte... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.I1, new byte[0], null);
        }
        return new SecsDataItem(SecsFormatCode.I1, values, null);
    }

    public static SecsDataItem ofU1(short... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.U1, new byte[0], null);
        }
        byte[] byteValues = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 0 || values[i] > 0xFF) {
                throw new IllegalArgumentException("U1 values must be between 0 and 0xFF");
            }
            byteValues[i] = (byte) values[i];
        }
        return new SecsDataItem(SecsFormatCode.U1, byteValues, null);
    }


    @Override
    public ByteBuf encode(ByteBufAllocator allocator) {
        ByteBuf dataBuf;
        int length;

        if (formatCode == SecsFormatCode.L) {
            dataBuf = allocator.buffer();
            if (listItems != null) {
                for (SECSII item : listItems) {
                    ByteBuf itemBuf = item.encode(allocator);
                    dataBuf.writeBytes(itemBuf);
                    itemBuf.release();
                }
            }
            length = listItems != null ? listItems.size() : 0;
        } else {
            dataBuf = (value != null) ? Unpooled.wrappedBuffer(value) : Unpooled.EMPTY_BUFFER;
            length = (value != null) ? value.length : 0;
        }

        int lengthBytesIndicator;
        if (length <= 0xFF) {
            lengthBytesIndicator = 1;
        } else if (length <= 0xFFFF) {
            lengthBytesIndicator = 2;
        } else if (length <= 0xFFFFFF) {
            lengthBytesIndicator = 3;
        } else {
            throw new IllegalArgumentException("Data item length " + length + " is too large for SECS-II");
        }

        ByteBuf headerBuf = allocator.buffer(1 + lengthBytesIndicator);
        headerBuf.writeByte(formatCode.getValue() | lengthBytesIndicator);

        switch (lengthBytesIndicator) {
            case 1 -> headerBuf.writeByte(length);
            case 2 -> headerBuf.writeShort(length);
            case 3 -> headerBuf.writeMedium(length);
        }
        
        return Unpooled.wrappedBuffer(headerBuf, dataBuf);
    }

    @Override
    public void decode(ByteBuf in) {
        if (!in.isReadable()) {
            this.listItems = Collections.emptyList();
            this.value = new byte[0];
            return;
        }

        byte formatByte = in.readByte();
        this.formatCode = SecsFormatCode.fromByte(formatByte);
        int lengthBytes = formatByte & 0x03;

        int length = switch (lengthBytes) {
            case 1 -> in.readUnsignedByte();
            case 2 -> in.readUnsignedShort();
            case 3 -> in.readUnsignedMedium();
            default -> throw new IllegalArgumentException("Invalid length bytes for data item: " + lengthBytes);
        };
        
        this.listItems = null; // Clear previous values
        this.value = null;

        if (formatCode == SecsFormatCode.L) {
            List<SECSII> decodedList = new ArrayList<>();
            ByteBuf subBuffer = in.readSlice(length); // This is where the error was, it should be totalLength
            // The previous code had a bug here. It should be based on number of items, not bytes for lists.
            // But the length *is* number of items for L. So it's not bytes.
            for (int i = 0; i < length; i++) {
                SecsDataItem item = new SecsDataItem();
                item.decode(in);
                decodedList.add(item);
            }
            this.listItems = decodedList;

        } else {
            this.value = new byte[length];
            in.readBytes(this.value);
        }
    }

    public SecsFormatCode getFormatCode() {
        return formatCode;
    }

    public List<SECSII> getListItems() {
        if (formatCode != SecsFormatCode.L) {
            throw new IllegalStateException("Not a List item.");
        }
        return listItems != null ? Collections.unmodifiableList(listItems) : Collections.emptyList();
    }
    
    public byte[] getRawBytes() {
        if (formatCode == SecsFormatCode.L) {
            throw new IllegalStateException("Cannot get raw bytes for a List item.");
        }
        return value;
    }

    public String getAscii(Charset charset) {
        if (formatCode != SecsFormatCode.A) {
            throw new IllegalStateException("Not an ASCII item.");
        }
        return (value != null) ? new String(value, charset) : "";
    }
    
    public String getAscii() {
        // Default to UTF-8 as requested, though US-ASCII is standard.
        return getAscii(StandardCharsets.UTF_8);
    }
    
    // Helper for reading from byte array
    private ByteBuffer getByteBuffer() {
        return (value != null) ? ByteBuffer.wrap(value) : ByteBuffer.allocate(0);
    }

    public long[] getU4() {
        if (formatCode != SecsFormatCode.U4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        long[] result = new long[bb.remaining() / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getInt() & 0xFFFFFFFFL;
        }
        return result;
    }
    
    public byte[] getBinary() {
        if (formatCode != SecsFormatCode.B) throw new IllegalStateException("Format is " + formatCode);
        return value;
    }

    public boolean[] getBoolean() {
        if (formatCode != SecsFormatCode.BOOLEAN) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        boolean[] result = new boolean[bb.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.get() != 0;
        }
        return result;
    }

    public long[] getI8() {
        if (formatCode != SecsFormatCode.I8) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        long[] result = new long[bb.remaining() / 8];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getLong();
        }
        return result;
    }
    
    public long[] getU8() {
        if (formatCode != SecsFormatCode.U8) throw new IllegalStateException("Format is " + formatCode);
        return getI8(); // Java doesn't have unsigned longs, so treat as signed for retrieval
    }
    
    public double[] getF8() {
        if (formatCode != SecsFormatCode.F8) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        double[] result = new double[bb.remaining() / 8];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getDouble();
        }
        return result;
    }

    public float[] getF4() {
        if (formatCode != SecsFormatCode.F4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        float[] result = new float[bb.remaining() / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getFloat();
        }
        return result;
    }
    
    public int[] getI4() {
        if (formatCode != SecsFormatCode.I4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        int[] result = new int[bb.remaining() / 4];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getInt();
        }
        return result;
    }
    
    public short[] getI2() {
        if (formatCode != SecsFormatCode.I2) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        short[] result = new short[bb.remaining() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getShort();
        }
        return result;
    }

    public int[] getU2() {
        if (formatCode != SecsFormatCode.U2) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        int[] result = new int[bb.remaining() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getShort() & 0xFFFF;
        }
        return result;
    }
    
    public byte[] getI1() {
        if (formatCode != SecsFormatCode.I1) throw new IllegalStateException("Format is " + formatCode);
        return value;
    }
    
    public short[] getU1() {
        if (formatCode != SecsFormatCode.U1) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        short[] result = new short[bb.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (short) (bb.get() & 0xFF);
        }
        return result;
    }
}
