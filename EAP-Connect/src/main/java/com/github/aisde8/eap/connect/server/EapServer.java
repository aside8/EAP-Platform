package com.github.aisde8.eap.connect.server;

import com.github.aisde8.eap.connect.client.EapClient;
import reactor.core.publisher.Flux;

public interface EapServer {

    void start();

    Flux<EapClient> accept();

    void shutdown();
}
