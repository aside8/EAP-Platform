package com.github.aside8.eap.protocol.hsms.enums;

import java.util.Arrays;

public enum DeselectStatus {

    COMMUNICATION_END(0),

    COMMUNICATION_NOT_ESTABLISHED(1),

    COMMUNICATION_BUSY(2);

    private final byte code;

    DeselectStatus(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public static DeselectStatus valueOf(int code) {
        return Arrays.stream(DeselectStatus.values()).filter(e -> e.code == code).findFirst().orElse(null);
    }
}
