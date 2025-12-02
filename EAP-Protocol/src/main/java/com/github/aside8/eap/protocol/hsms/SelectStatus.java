package com.github.aside8.eap.protocol.hsms;

/**
 * Enumerates the possible status codes for a Select.rsp message,
 * which are conveyed in the P-Type field of the HSMS header.
 */
public enum SelectStatus {
    /**
     * Status 0: Connection Established. The selection was successful.
     */
    CONNECTION_ESTABLISHED(0),

    /**
     * Status 1: Connection Not Ready. The entity is not ready to communicate.
     */
    CONNECTION_NOT_READY(1),

    /**
     * Status 2: Session ID Not Found. The requested Session ID (Device ID) is unknown.
     */
    SESSION_ID_NOT_FOUND(2),

    /**
     * Status 3: Already Selected. The entity is already in a selected state.
     */
    ALREADY_SELECTED(3);

    private final byte code;

    SelectStatus(int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }
}
