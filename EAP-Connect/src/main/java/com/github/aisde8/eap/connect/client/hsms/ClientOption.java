package com.github.aisde8.eap.connect.client.hsms;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClientOption {

    private String host;

    private int port;

    private int deviceId;

    @Builder.Default
    private TimeConfig timeConfig = new TimeConfig();
}
