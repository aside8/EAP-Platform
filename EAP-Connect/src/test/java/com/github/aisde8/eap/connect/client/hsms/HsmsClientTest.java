package com.github.aisde8.eap.connect.client.hsms;

import com.github.aside8.eap.protocol.hsms.HsmsHeader;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessageType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HsmsClientTest {

    @Test
    @Disabled
    void testConnect() throws InterruptedException {
        HsmsClient hsmsClient = new HsmsClient(ClientOption.builder().host("172.16.57.40").port(9401).build());
        hsmsClient.receive().map(message -> (HsmsMessage) message)
                .subscribe(message -> System.out.println(message.toString()));
        hsmsClient.connect().block();
        HsmsMessage hsmsMessage = new HsmsMessage();
        HsmsHeader header = new HsmsHeader();
        header.setSessionId((short) 0);
        header.setPtype((byte) 0);
        header.setStype((byte) HsmsMessageType.SELECT_REQ.getSType());
        header.setStream((byte) 0);
        header.setFunction((byte) 0);
        header.setSystemBytes((byte) 0);
        hsmsMessage.setHeader(header);
        hsmsClient.sendRequest(hsmsMessage);
        Thread.sleep(1000 * 60);
    }
}