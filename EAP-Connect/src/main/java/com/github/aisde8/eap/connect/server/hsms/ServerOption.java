package com.github.aisde8.eap.connect.server.hsms;

import io.netty.channel.EventLoopGroup;
import lombok.Data;

@Data
public class ServerOption {

    private String host;

    private int port;

    private EventLoopGroup eventLoopGroup;
}
