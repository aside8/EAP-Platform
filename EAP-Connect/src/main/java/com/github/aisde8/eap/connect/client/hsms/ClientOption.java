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
}
