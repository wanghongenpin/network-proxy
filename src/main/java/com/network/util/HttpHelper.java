package com.network.util;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;

/**
 * @author wanghongen
 * 2023/5/25
 */
public interface HttpHelper {


    /**
     * 获取主机名
     */
    static String getHost(HttpRequest httpRequest) {
        return httpRequest.uri().split(":")[0];
    }

    /**
     * 获取主机和端口
     */
    static HostAndPort getHostAndPort(HttpRequest request) {
        String requestUri = request.uri();
        //有些请求直接是路径 /xxx, 从header取host
        if (request.uri().startsWith("/")) {
            requestUri = request.headers().get(HttpHeaderNames.HOST);
        }
        if (requestUri == null) {
            return null;
        }
        return HostAndPort.of(requestUri);
    }

}
