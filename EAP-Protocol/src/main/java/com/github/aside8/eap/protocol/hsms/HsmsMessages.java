package com.github.aside8.eap.protocol.hsms;

import com.github.aside8.eap.protocol.secs2.SECSII;

public class HsmsMessages {

    public static HsmsMessage dataReq(int deviceId, int stream, int function, int systemBytes, SECSII data) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) deviceId)
                .ptype((byte) HsmsMessageType.DATA_MESSAGE.getPType())
                .stype((byte) HsmsMessageType.DATA_MESSAGE.getSType())
                .stream((byte) stream)
                .function((byte) function)
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, data);
    }

    public static HsmsMessage dataRes(HsmsMessage req, SECSII data) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .ptype((byte) HsmsMessageType.DATA_MESSAGE.getPType())
                .stype((byte) HsmsMessageType.DATA_MESSAGE.getSType())
                .stream(req.getHeader().getStream())
                .function((byte) (req.getHeader().getFunction() + 1))
                .systemBytes(req.getHeader().getSystemBytes())
                .build();
        return new HsmsMessage(header, data);
    }

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
