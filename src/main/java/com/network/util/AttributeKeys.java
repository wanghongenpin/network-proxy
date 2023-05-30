package com.network.util;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.AttributeKey;

/**
 * @author wanghongen
 * 2023/5/23
 */
public interface AttributeKeys {
    AttributeKey<HostAndPort> HOST_ATTRIBUTE_KEY = AttributeKey.newInstance("HOST");
    AttributeKey<HttpRequest> REQUEST_KEY = AttributeKey.newInstance(HttpRequest.class.getName());
}
