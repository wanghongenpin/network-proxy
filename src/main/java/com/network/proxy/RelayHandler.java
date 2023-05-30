package com.network.proxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author wanghongen
 * 2023/5/24
 */
public class RelayHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(RelayHandler.class);
    private final Channel remoteChannel;

    public RelayHandler(Channel remoteChannel) {
        this.remoteChannel = remoteChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        remoteChannel.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("relay handler error", cause);
        flushAndClose(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        /*
         * 连接断开时关闭另一端连接。
         * 如果代理到远端服务器连接断了也同时关闭代理到客户的连接。
         * 如果代理到客户端的连接断了也同时关闭代理到远端服务器的连接。
         */
        flushAndClose(remoteChannel);
    }

    private void flushAndClose(Channel ch) {
        if (ch != null && ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
