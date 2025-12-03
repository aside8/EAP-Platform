package com.github.aside8.eap.protocol.secs2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class SecsDataItem implements SECSII {
    @Getter
    private SecsFormatCode formatCode;
    private byte[] value;
    private List<SECSII> listItems;

    public SecsDataItem() {
        this.formatCode = SecsFormatCode.LIST; // Default to List, will be updated during decode
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
    public static SecsDataItem list(SECSII... items) {
        List<SECSII> itemList = (items != null) ? Arrays.asList(items) : Collections.emptyList();
        return new SecsDataItem(SecsFormatCode.LIST, null, itemList);
    }

    public static SecsDataItem binary(byte... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.BINARY, new byte[0], null);
        }
        return new SecsDataItem(SecsFormatCode.BINARY, values, null);
    }

    public static SecsDataItem bool(boolean... values) {
        if (values == null) {
            return new SecsDataItem(SecsFormatCode.BOOLEAN, new byte[0], null);
        }
        byte[] byteValues = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            byteValues[i] = (byte) (values[i] ? 1 : 0);
        }
        return new SecsDataItem(SecsFormatCode.BOOLEAN, byteValues, null);
    }

    public static SecsDataItem ascii(String value, Charset charset) {
        if (value == null) {
            return new SecsDataItem(SecsFormatCode.ASCII, new byte[0], null);
        }
        return new SecsDataItem(SecsFormatCode.ASCII, value.getBytes(charset), null);
    }

    public static SecsDataItem ascii(String value) {
        // Per SECS standard, 'A' format is US-ASCII.
        return ascii(value, StandardCharsets.US_ASCII);
    }


    public static SecsDataItem int8(long... values) {
        SecsFormatCode formatCode = SecsFormatCode.INT8;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (long value : values) {
            bb.putLong(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem int1(byte... values) {
        SecsFormatCode formatCode = SecsFormatCode.INT1;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        return new SecsDataItem(formatCode, values, null);
    }

    public static SecsDataItem int2(short... values) {
        SecsFormatCode formatCode = SecsFormatCode.INT2;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (short value : values) {
            bb.putShort(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem int4(int... values) {
        SecsFormatCode formatCode = SecsFormatCode.INT4;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (int value : values) {
            bb.putInt(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem float8(double... values) {
        SecsFormatCode formatCode = SecsFormatCode.FLOAT8;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (double value : values) {
            bb.putDouble(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem float4(float... values) {
        SecsFormatCode formatCode = SecsFormatCode.FLOAT4;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (float value : values) {
            bb.putFloat(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem uint8(long... values) {
        SecsFormatCode formatCode = SecsFormatCode.UINT8;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        // ByteBuffer handles 8-byte longs directly
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (long value : values) {
            bb.putLong(value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem uint1(short... values) {
        SecsFormatCode formatCode = SecsFormatCode.UINT1;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        byte[] byteValues = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] < 0 || values[i] > 0xFF) {
                throw new IllegalArgumentException("U1 values must be between 0 and 0xFF");
            }
            byteValues[i] = (byte) values[i];
        }
        return new SecsDataItem(formatCode, byteValues, null);
    }

    public static SecsDataItem uint2(int... values) {
        SecsFormatCode formatCode = SecsFormatCode.UINT2;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }
        for (int value : values) {
            if (value < 0 || value > 0xFFFF) {
                throw new IllegalArgumentException("U2 values must be between 0 and 0xFFFF");
            }
        }
        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (int value : values) {
            bb.putShort((short) value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    public static SecsDataItem uint4(long... values) {
        SecsFormatCode formatCode = SecsFormatCode.UINT4;
        if (values == null) {
            return new SecsDataItem(formatCode, new byte[0], null);
        }

        for (long value : values) {
            if (value < 0 || value > 0xFFFFFFFFL) {
                throw new IllegalArgumentException("U4 values must be between 0 and 0xFFFFFFFFL");
            }
        }

        ByteBuffer bb = ByteBuffer.allocate(values.length * formatCode.getSize());
        for (long value : values) {
            bb.putInt((int) value);
        }
        return new SecsDataItem(formatCode, bb.array(), null);
    }

    @Override
    public ByteBuf encode(ByteBufAllocator allocator) {
        ByteBuf dataBuf;
        int length;

        if (formatCode == SecsFormatCode.LIST) {
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

        if (formatCode == SecsFormatCode.LIST) {
            List<SECSII> decodedList = new ArrayList<>();
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

    public byte[] getRawBytes() {
        if (formatCode == SecsFormatCode.LIST) {
            throw new IllegalStateException("Cannot get raw bytes for a List item.");
        }
        return value;
    }

    // Helper for reading from byte array
    private ByteBuffer getByteBuffer() {
        return (value != null) ? ByteBuffer.wrap(value) : ByteBuffer.allocate(0);
    }


    public List<SECSII> getList() {
        if (formatCode != SecsFormatCode.LIST) {
            throw new IllegalStateException("Not a List item.");
        }
        return listItems != null ? Collections.unmodifiableList(listItems) : Collections.emptyList();
    }

    public SECSII get(int index) {
        List<SECSII> list = getList();
        if (index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    public byte[] getBinary() {
        if (formatCode != SecsFormatCode.BINARY) throw new IllegalStateException("Format is " + formatCode);
        return value;
    }

    @Override
    public byte getBinary(int index) {
        byte[] binary = getBinary();
        if (index < 0 || index >= binary.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + binary.length);
        }
        return binary[index];
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

    @Override
    public boolean getBoolean(int index) {
        boolean[] booleans = getBoolean();
        if (index < 0 || index >= booleans.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + booleans.length);
        }
        return booleans[index];
    }

    public String getAscii(Charset charset) {
        if (formatCode != SecsFormatCode.ASCII) {
            throw new IllegalStateException("Not an ASCII item.");
        }
        return (value != null) ? new String(value, charset) : "";
    }
    
    public String getAscii() {
        // Default to UTF-8 as requested, though US-ASCII is standard.
        return getAscii(StandardCharsets.UTF_8);
    }

    public long[] getInt8() {
        if (formatCode != SecsFormatCode.INT8) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        long[] result = new long[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getLong();
        }
        return result;
    }
    @Override
    public long getInt8(int index) {
        long[] int8s = getInt8();
        if (index < 0 || index >= int8s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + int8s.length);
        }
        return int8s[index];
    }

    public byte[] getInt1() {
        if (formatCode != SecsFormatCode.INT1) throw new IllegalStateException("Format is " + formatCode);
        return value;
    }

    @Override
    public byte getInt1(int index) {
        byte[] int1s = getInt1();
        if (index < 0 || index >= int1s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + int1s.length);
        }
        return int1s[index];
    }

    public short[] getInt2() {
        if (formatCode != SecsFormatCode.INT2) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        short[] result = new short[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getShort();
        }
        return result;
    }

    @Override
    public short getInt2(int index) {
        short[] int2s = getInt2();
        if (index < 0 || index >= int2s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + int2s.length);
        }
        return int2s[index];
    }

    public int[] getInt4() {
        if (formatCode != SecsFormatCode.INT4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        int[] result = new int[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getInt();
        }
        return result;
    }

    @Override
    public int getInt4(int index) {
        int[] int4s = getInt4();
        if (index < 0 || index >= int4s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + int4s.length);
        }
        return int4s[index];
    }

    public double[] getFloat8() {
        if (formatCode != SecsFormatCode.FLOAT8) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        double[] result = new double[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getDouble();
        }
        return result;
    }

    @Override
    public float getFloat8(int index) {
        double[] float8s = getFloat8();
        if (index < 0 || index >= float8s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + float8s.length);
        }
        return (float) float8s[index];
    }

    public float[] getFloat4() {
        if (formatCode != SecsFormatCode.FLOAT4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        float[] result = new float[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getFloat();
        }
        return result;
    }
    @Override
    public float getFloat4(int index) {
        float[] float4s = getFloat4();
        if (index < 0 || index >= float4s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + float4s.length);
        }
        return float4s[index];
    }

    public long[] getUint8() {
        if (formatCode != SecsFormatCode.UINT8) throw new IllegalStateException("Format is " + formatCode);
        return getInt8(); // Java doesn't have unsigned longs, so treat as signed for retrieval
    }

    @Override
    public long getUint8(int index) {
        long[] uint8s = getUint8();
        if (index < 0 || index >= uint8s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + uint8s.length);
        }
        return uint8s[index];
    }

    public short[] getUint1() {
        if (formatCode != SecsFormatCode.UINT1) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        short[] result = new short[bb.remaining()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (short) (bb.get() & 0xFF);
        }
        return result;
    }

    @Override
    public short getUint1(int index) {
        short[] uint1s = getUint1();
        if (index < 0 || index >= uint1s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + uint1s.length);
        }
        return uint1s[index];
    }

    public int[] getUint2() {
        if (formatCode != SecsFormatCode.UINT2) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        int[] result = new int[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getShort() & 0xFFFF;
        }
        return result;
    }

    @Override
    public int getUint2(int index) {
        int[] uint2s = getUint2();
        if (index < 0 || index >= uint2s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + uint2s.length);
        }
        return uint2s[index];
    }

    public long[] getUint4() {
        if (formatCode != SecsFormatCode.UINT4) throw new IllegalStateException("Format is " + formatCode);
        ByteBuffer bb = getByteBuffer();
        long[] result = new long[bb.remaining() / formatCode.getSize()];
        for (int i = 0; i < result.length; i++) {
            result[i] = bb.getInt() & 0xFFFFFFFFL;
        }
        return result;
    }
    @Override
    public long getUint4(int index) {
        long[] uint4s = getUint4();
        if (index < 0 || index >= uint4s.length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + uint4s.length);
        }
        return uint4s[index];
    }

    @Override
    public String toString() {
        return toFormatString();
    }

    public String toFormatString() {
        StringBuilder sb = new StringBuilder();
        toFormatString(sb, 0);
        return sb.toString();
    }

    private void toFormatString(StringBuilder sb, int indent) {
        sb.append("  ".repeat(Math.max(0, indent)));
        sb.append("<").append(formatCode.getSymbol()).append(" [");

        if (formatCode == SecsFormatCode.LIST) {
            int size = (listItems != null) ? listItems.size() : 0;
            sb.append(size).append("]");

            if (size == 0) {
                sb.append(">\n");
                return;
            }

            sb.append("\n");

            for (SECSII item : listItems) {
                if (item instanceof SecsDataItem) {
                    ((SecsDataItem) item).toFormatString(sb, indent + 1);
                }
            }
            sb.append("  ".repeat(Math.max(0, indent)));
            sb.append(">\n");

        } else {
            int length = (value != null) ? value.length : 0;
            int size = (formatCode.getSize() > 0 && length > 0) ? length / formatCode.getSize() : length;
            sb.append(size).append("] ");

            if (value != null) {
                switch (formatCode) {
                    case ASCII:
                        sb.append("\"").append(getAscii()).append("\"");
                        break;
                    case INT1:
                        sb.append(Arrays.toString(getInt1()).replaceAll("[\\[\\],]", ""));
                        break;
                    case INT2:
                        sb.append(Arrays.toString(getInt2()).replaceAll("[\\[\\],]", ""));
                        break;
                    case INT4:
                        sb.append(Arrays.toString(getInt4()).replaceAll("[\\[\\],]", ""));
                        break;
                    case INT8:
                        sb.append(Arrays.toString(getInt8()).replaceAll("[\\[\\],]", ""));
                        break;
                    case UINT1:
                        sb.append(Arrays.toString(getUint1()).replaceAll("[\\[\\],]", ""));
                        break;
                    case UINT2:
                        sb.append(Arrays.toString(getUint2()).replaceAll("[\\[\\],]", ""));
                        break;
                    case UINT4:
                        sb.append(Arrays.toString(getUint4()).replaceAll("[\\[\\],]", ""));
                        break;
                    case UINT8:
                        sb.append(Arrays.toString(getUint8()).replaceAll("[\\[\\],]", ""));
                        break;
                    case FLOAT4:
                        sb.append(Arrays.toString(getFloat4()).replaceAll("[\\[\\],]", ""));
                        break;
                    case FLOAT8:
                        sb.append(Arrays.toString(getFloat8()).replaceAll("[\\[\\],]", ""));
                        break;
                    case BOOLEAN:
                        sb.append(Arrays.toString(getBoolean()).replaceAll("[\\[\\],]", ""));
                        break;
                    case BINARY:
                        StringBuilder hex = new StringBuilder();
                        for (byte b : value) {
                            hex.append(String.format("0x%02X ", b));
                        }
                        sb.append(hex.toString().trim());
                        break;
                    default:
                        sb.append("...");
                        break;
                }
            }
            sb.append(">\n");
        }
    }
}
