package com.github.aisde8.eap.connect.client.hsms;

import com.github.aisde8.eap.connect.client.EapClient;
import com.github.aisde8.eap.connect.client.Reply;
import com.github.aside8.eap.protocol.hsms.HsmsMessages;
import com.github.aside8.eap.protocol.secs2.SECSII;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import com.github.aisde8.eap.connect.client.EapClientManager;

import java.time.Duration;

class HsmsClientNewApiTest {

    @Test
    void systemBytesAreUnique_for_consecutive_sends_and_requests() {
        var ch = new EmbeddedChannel();
        var client = new HsmsClient(ch, ClientOption.builder().deviceId(1).build(), new EapClientManager());

        var req1 = HsmsMessages.dataReq(0, true, 1, 1, 0, SECSII.ascii("a"));
        var req2 = HsmsMessages.dataReq(0, true, 1, 1, 0, SECSII.ascii("b"));

        var mono1 = client.sendRequest(req1, Duration.ofSeconds(1));
        // systemBytes are assigned synchronously before write
        org.junit.jupiter.api.Assertions.assertTrue(req1.getSystemBytes() != 0);
        var d1 = mono1.subscribe();
        d1.dispose();

        var mono2 = client.sendRequest(req2, Duration.ofSeconds(1));
        org.junit.jupiter.api.Assertions.assertTrue(req2.getSystemBytes() > req1.getSystemBytes());
        var d2 = mono2.subscribe();
        d2.dispose();
    }

    @Test
    void pendingRepliesRemoved_on_dispose_and_disconnect() {
        var ch = new EmbeddedChannel();
        var client = new HsmsClient(ch, ClientOption.builder().deviceId(1).build(), new EapClientManager());

        var req = HsmsMessages.dataReq(0, true, 1, 1, 0, SECSII.ascii("x"));
        var mono = client.sendRequest(req, Duration.ofSeconds(5));

        var disp = mono.subscribe();
        disp.dispose(); // simulate caller cancelling

        // pendingReplies should be cleaned up
        org.junit.jupiter.api.Assertions.assertTrue(client.getPendingReplies().isEmpty());

        // now simulate disconnect and ensure no exceptions
        client.disconnect().block();
    }
}