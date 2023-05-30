package com.network.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * @author wanghongen
 * 2023/5/26
 */
public interface ChannelUtils {

    /**
     * 清除所有handler
     */
    static Channel clearHandler(Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();
        while (pipeline.last() != null)
            pipeline.removeLast();

        return channel;
    }
}
