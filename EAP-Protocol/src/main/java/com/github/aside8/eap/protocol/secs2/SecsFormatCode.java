package com.github.aside8.eap.protocol.secs2;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SecsFormatCode {
    L((byte) 0x00),
    B((byte) 0x10),
    BOOLEAN((byte) 0x20),
    A((byte) 0x40),
    I8((byte) 0x60),
    U8((byte) 0x70),
    F8((byte) 0x80),
    F4((byte) 0x90),
    I4((byte) 0xA0),
    U4((byte) 0xB0),
    I2((byte) 0xC0),
    U2((byte) 0xD0),
    I1((byte) 0xE0),
    U1((byte) 0xF0);

    private final byte value;

    private static final Map<Byte, SecsFormatCode> FORMAT_CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(SecsFormatCode::getValue, Function.identity()));

    SecsFormatCode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
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
