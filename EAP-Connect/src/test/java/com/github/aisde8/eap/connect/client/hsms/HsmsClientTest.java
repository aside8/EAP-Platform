package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.EapClientManager;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SECSII;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class HsmsClientTest {

    @Test
    @Disabled
    void testConnect() throws InterruptedException {
        EapClientManager eapClientManager = new EapClientManager();
        HsmsClient hsmsClient = new HsmsClient(ClientOption.builder().host("127.0.0.1").port(5000)
        .eventLoopGroup(new NioEventLoopGroup()).build(),  eapClientManager);
        
        hsmsClient.connect().block(); // 确保连接成功

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        Mono<Boolean> sendMono = hsmsClient.send(HsmsMessages.dataReq(0, false, 1, 1, 0, SECSII.ascii("123")));
        sendMono.subscribe(x -> System.out.println("send success: " + x), Throwable::printStackTrace);
        boolean completedInTime = latch.await(10, TimeUnit.SECONDS);
    }
}