package com.github.aside8.eap.protocol.secs2;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum SecsFormatCode {
    LIST((byte) 0x00, -1,"L"),
    BINARY((byte) 0x20, 1,"B"),
    BOOLEAN((byte) 0x24, 1,"BOOLEAN"),
    ASCII((byte) 0x40, 1,"A"),
    JIS8((byte) 0x44, 1,"J"),
    UNICODE((byte) 0x48, 2,"UNICODE"),
    INT8((byte) 0x60, 8,"I8"),
    INT1((byte) 0x64, 1,"I1"),
    INT2((byte) 0x68, 2,"I2"),
    INT4((byte) 0x70, 4,"I4"),
    FLOAT8((byte) 0x80, 8,"F8"),
    FLOAT4((byte) 0x90, 4,"F4"),
    UINT8((byte) 0xA0, 8,"U8"),
    UINT1((byte) 0xA4, 1,"U1"),
    UINT2((byte) 0xA8, 2,"U2"),
    UINT4((byte) 0xB0, 4,"U4");

    private byte value;

    private int size;

    private  String symbol;

    private static final Map<Byte, SecsFormatCode> FORMAT_CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(SecsFormatCode::getValue, Function.identity()));

    SecsFormatCode(byte value, Integer size, String symbol) {
        this.value = value;
        this.size = size;
        this.symbol = symbol;
    }

    public static SecsFormatCode fromByte(byte code) {
        // Mask out the length bits
        byte formatCode = (byte) (code & 0xFC);
        SecsFormatCode secsFormatCode = FORMAT_CODE_MAP.get(formatCode);
        if (secsFormatCode == null) {
            throw new IllegalArgumentException("Unsupported SECS-II format code: " + String.format("0x%02X", formatCode));
        }
        return secsFormatCode;
    }
}
