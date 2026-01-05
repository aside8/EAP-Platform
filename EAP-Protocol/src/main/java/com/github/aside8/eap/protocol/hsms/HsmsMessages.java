package com.github.aside8.eap.protocol.hsms;

import com.github.aside8.eap.protocol.hsms.enums.DeselectStatus;
import com.github.aside8.eap.protocol.hsms.enums.HsmsMessageType;
import com.github.aside8.eap.protocol.hsms.enums.SelectStatus;
import com.github.aside8.eap.protocol.secs2.SECSII;

public class HsmsMessages {

    public static HsmsMessage dataReq(int deviceId, boolean wbit, int stream, int function, int systemBytes, SECSII data) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) deviceId)
                .wbit(wbit)
                .stream((byte) stream)
                .function((byte) function)
                .ptype((byte) HsmsMessageType.DATA_MESSAGE.getPType())
                .stype((byte) HsmsMessageType.DATA_MESSAGE.getSType())
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, data);
    }

    public static HsmsMessage dataRes(HsmsMessage req, SECSII data) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .wbit(false)
                .stream(req.getHeader().getStream())
                .function((byte) (req.getHeader().getFunction() + 1))
                .ptype((byte) HsmsMessageType.DATA_MESSAGE.getPType())
                .stype((byte) HsmsMessageType.DATA_MESSAGE.getSType())
                .systemBytes(req.getHeader().getSystemBytes())
                .build();
        return new HsmsMessage(header, data);
    }

    public static HsmsMessage selectReq(int deviceId, int systemBytes) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) deviceId)
                .stream((byte) 0x00)
                .function((byte) 0x00)
                .ptype((byte) HsmsMessageType.SELECT_REQ.getPType())
                .stype((byte) HsmsMessageType.SELECT_REQ.getSType())
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage selectResp(HsmsMessage req, SelectStatus selectStatus) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .wbit(false)
                .stream((byte) 0x00)
                .function(selectStatus.getCode())
                .ptype((byte) HsmsMessageType.SELECT_RSP.getPType())
                .stype((byte) HsmsMessageType.SELECT_RSP.getSType())
                .systemBytes(req.getHeader().getSystemBytes())
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage deselectReq(int deviceId, int systemBytes) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId((short) deviceId)
                .wbit(false)
                .stream((byte) 0x00)
                .function((byte) 0x00)
                .ptype((byte) HsmsMessageType.DESELECT_REQ.getPType())
                .stype((byte) HsmsMessageType.DESELECT_REQ.getSType())
                .systemBytes(systemBytes)
                .build();
        return new HsmsMessage(header, null);
    }

    public static HsmsMessage deselectResp(HsmsMessage req, DeselectStatus deselectStatus) {
        HsmsHeader header = HsmsHeader.builder()
                .sessionId(req.getHeader().getSessionId())
                .wbit(false)
                .stream((byte) 0x00)
                .function(deselectStatus.getCode())
                .ptype((byte) HsmsMessageType.DESELECT_RSP.getPType())
                .stype((byte) HsmsMessageType.DESELECT_RSP.getSType())
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
