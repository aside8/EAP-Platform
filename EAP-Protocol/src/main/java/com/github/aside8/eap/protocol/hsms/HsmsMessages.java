package com.github.aside8.eap.protocol.hsms;

public class HsmsMessages {

    public static HsmsMessage selectReq(int systemBytes) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) 0xFFFF)
                .ptype((byte) HsmsMessageType.SELECT_REQ.getPType())
                .stype((byte) HsmsMessageType.SELECT_REQ.getSType())
                .stream((byte) 0x00)
                .function((byte) 0x00)
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage selectResp(HsmsMessage req, SelectStatus selectStatus) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .ptype((byte) HsmsMessageType.SELECT_RSP.getPType())
                .stype((byte) HsmsMessageType.SELECT_RSP.getSType())
                .stream(selectStatus.getCode())
                .function((byte) 0x00)
                .systemBytes(req.getHeader().getSystemBytes())
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage linkTestReq(int systemBytes) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) 0xFFFF)
                .ptype((byte) HsmsMessageType.LINK_TEST_REQ.getPType())
                .stype((byte) HsmsMessageType.LINK_TEST_REQ.getSType())
                .stream((byte) 0x00)
                .function((byte) 0x00)
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage linkTestResp(HsmsMessage req) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .ptype((byte) HsmsMessageType.LINK_TEST_RSP.getPType())
                .stype((byte) HsmsMessageType.LINK_TEST_RSP.getSType())
                .stream((byte) 0x00)
                .function((byte) 0x00)
                .systemBytes(req.getHeader().getSystemBytes())
                .build();
        return new HsmsMessage(header, null);
    }
}
