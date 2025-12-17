package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aside8.eap.protocol.hsms.HsmsMessage;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SECSII;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class HsmsClientTest {

    @Test
    @Disabled
    void testConnect() throws InterruptedException {
        EapClientManager eapClientManager = new EapClientManager();
        HsmsClient hsmsClient = new HsmsClient(ClientOption.builder().host("127.0.0.1").port(5000)
        .eventLoopGroup(new NioEventLoopGroup()).build(),  eapClientManager);
        hsmsClient.receive().map(message -> (HsmsMessage) message)
                .subscribe(message -> System.out.println(message.toString()));
        hsmsClient.connect().block();
        Thread.sleep(1000 * 10);
        hsmsClient.send(HsmsMessages.dataReq(0, false, 1, 1, 0, SECSII.ascii("123")))
                        .doOnSuccess(x -> System.out.println("success"))
                                .doOnError(x -> System.out.println("error"));
        Thread.sleep(1000 * 600);
    }
}