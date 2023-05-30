package com.network.client;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wanghongen
 * 2023/5/23
 */
public class HttpConnectionPool {
    // 单例
    public static final HttpConnectionPool INSTANCE = new HttpConnectionPool();

    /**
     * 连接池
     */
    private final ConcurrentHashMap<String, Channel> CONNECTION_POOL = new ConcurrentHashMap<>();

    /**
     * 添加连接
     */
    public Channel add(String clientId, Channel channel) {
        return CONNECTION_POOL.put(clientId, channel);
    }

    /**
     * 获取连接
     */
    public Channel getChannel(String clientId) {
        return CONNECTION_POOL.get(clientId);
    }

    /**
     * 是否存在连接
     */
    public boolean contains(String clientId) {
        return CONNECTION_POOL.containsKey(clientId);
    }

    /**
     * 移除连接
     */
    public Channel remove(String clientId) {
        return CONNECTION_POOL.remove(clientId);
    }

}

