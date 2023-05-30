package com.network.client;

import com.network.util.CertificateManager;
import com.network.util.HostAndPort;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslHandler;

import javax.net.ssl.SSLEngine;

/**
 * @author wanghongen
 * 2023/5/23
 */
public class HttpClients {

    /**
     * 建立连接
     */
    public static ChannelFuture connect(HostAndPort hostAndPort, EventLoopGroup eventLoopGroup) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if (hostAndPort.isSsl()) {
                            //客户端ssl
                            SSLEngine engine = CertificateManager.getInstance().getClientSslContext().newEngine(ch.alloc(), hostAndPort.getHost(), hostAndPort.getPort());
                            ch.pipeline().addFirst(new SslHandler(engine));
                        }
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(1024 * 10 * 1024));
                    }
                });


        return bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort());
    }


}
