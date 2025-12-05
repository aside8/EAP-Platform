package com.github.aisde8.eap.connect.client.hsms;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientOption {

    @Builder.Default
    public int connectTimeoutMillis = 5000;

    private String host;

    private int port;
}
