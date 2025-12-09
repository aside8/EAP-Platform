package com.github.aside8.eap.protocol.hsms;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HsmsMessageType {
    /**
     * HSMS Data Message (pType = 0, sType = 0)
     */
    DATA_MESSAGE(0, 0),
    /**
     * SELECT.REQ (pType = 0, sType = 1)
     */
    SELECT_REQ(0, 1),
    /**
     * SELECT.RSP (pType = 0, sType = 2)
     */
    SELECT_RSP(0, 2),
    /**
     * DESELECT.REQ (pType = 0, sType = 3)
     */
    DESELECT_REQ(0, 3),
    /**
     * DESELECT.RSP (pType = 0, sType = 4)
     */
    DESELECT_RSP(0, 4),
    /**
     * LINKTEST.REQ (pType = 0, sType = 5)
     */
    LINK_TEST_REQ(0, 5),
    /**
     * LINK_TEST.RSP (pType = 0, sType = 6)
     */
    LINK_TEST_RSP(0, 6),
    /**
     * ABORT.REQ (pType = 0, sType = 9)
     */
    ABORT_REQ(0, 9);

    private final int pType;

    private final int sType;

    private static final Map<Integer, HsmsMessageType> S_TYPE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(HsmsMessageType::getSType, Function.identity()));

    HsmsMessageType(int pType, int sType) {
        this.pType = pType;
        this.sType = sType;
    }

    public int getPType() {
        return pType;
    }

    public int getSType() {
        return sType;
    }

    public static HsmsMessageType fromSType(int sType) {
        return S_TYPE_MAP.getOrDefault(sType, DATA_MESSAGE);
    }
}
