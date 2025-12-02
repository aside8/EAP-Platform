package com.github.aside8.eap.protocol;

import java.io.Serializable;

/**
 * 通用消息接口/基类
 * A message is a serializable object that can be encoded and decoded.
 */
public interface Message extends Codec, Traceable, Serializable {

    Protocol getProtocol();
}
