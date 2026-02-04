package com.github.aisde8.eap.connect.client.hsms;

import io.netty.channel.EventLoopGroup;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientOption {

    private String host;

    private int port;

    private int deviceId;

    private String clientId;

    private EventLoopGroup eventLoopGroup;

    @Builder.Default
    private TimeConfig timeConfig = new TimeConfig();

    /**
     * HSMS framing: whether the 4-byte length field includes the length of the length field itself.
     * Default: false (legacy behaviour).
     */
    @Builder.Default
    private boolean includeLength = false;
}
