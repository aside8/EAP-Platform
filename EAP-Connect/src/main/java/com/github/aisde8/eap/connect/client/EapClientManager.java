package com.github.aisde8.eap.connect.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EapClientManager {

    private static final Logger logger = LoggerFactory.getLogger(EapClientManager.class);

    private final Map<String, EapClient> clients = new ConcurrentHashMap<>();

    public void addClient(String host, int port, EapClient client) {
        String key = generateKey(host, port);
        clients.put(key, client);
        logger.info("Client added: {}", key);
    }

    public void removeClient(String host, int port) {
        String key = generateKey(host, port);
        clients.remove(key);
        logger.info("Client removed: {}", key);
    }

    public EapClient getClient(String host, int port) {
        String key = generateKey(host, port);
        return clients.get(key);
    }

    public Map<String, EapClient> getAllClients() {
        return clients;
    }

    private String generateKey(String host, int port) {
        return host + ":" + port;
    }
}
