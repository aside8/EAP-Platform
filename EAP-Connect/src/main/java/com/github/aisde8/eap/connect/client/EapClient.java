package com.github.aisde8.eap.connect.client;

import com.github.aside8.eap.protocol.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EapClient {
    /**
     * 异步连接到服务器
     * @return a Mono that completes when the connection is established or errors.
     */
    Mono<Void> connect();

    /**
     * 断开连接
     * @return a Mono that completes when the disconnection is finished.
     */
    Mono<Void> disconnect();

    /**
     * 接收消息
     * @return a flux of incoming messages.
     */
    Flux<Message> receive();

    /**
     * 发送一个消息后不等待响应 (Fire-and-Forget)
     * @param message 消息对象
     * @return a Mono that completes when the message has been sent (flushed) or errors.
     */
    Mono<Void> send(Message message);

    /**
     * 发送一个请求并等待一个响应 (Request-Response)
     * @param request The request message.
     * @return A Mono that will complete with the corresponding reply message.
     */
    Mono<Message> sendRequest(Message request);

    /**
     * 客户端是否处于连接状态
     * @return true 如果已连接
     */
    boolean isConnected();
}
