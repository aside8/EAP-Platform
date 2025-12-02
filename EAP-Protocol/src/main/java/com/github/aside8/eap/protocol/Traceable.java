package com.github.aside8.eap.protocol;

public interface Traceable {

    /**
     * 获取消息的跟踪ID
     * @return 跟踪ID
     */
    String getTraceId();
}
