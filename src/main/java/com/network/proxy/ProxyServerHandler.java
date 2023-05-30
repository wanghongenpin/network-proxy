package com.network.proxy;

import com.network.client.HttpClients;
import com.network.client.HttpConnectionPool;
import com.network.util.AttributeKeys;
import com.network.util.CertificateManager;
import com.network.util.ChannelUtils;
import com.network.util.HostAndPort;
import com.network.util.HostFilter;
import com.network.util.HttpHelper;
import com.network.util.ObjectUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * @author wanghongen
 * 2023/5/9
 */
@ChannelHandler.Sharable
public class ProxyServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ProxyServerHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            try {
                forward(ctx, (HttpRequest) msg);
            } catch (Exception e) {
                log.error("转发请求失败 [{}]", msg, e);
            }
        } else {
            super.channelRead(ctx, msg);
        }
    }

    /**
     * 获取远程连接
     */
    protected ChannelFuture getRemoteChannel(Channel clientChannel, HttpRequest httpRequest) {
        HostAndPort hostAndPort = ObjectUtil.requireNonNullElseGet(HttpHelper.getHostAndPort(httpRequest),
                () -> clientChannel.attr(AttributeKeys.HOST_ATTRIBUTE_KEY).get());
        String clientId = clientChannel.id().asShortText();
        //客户端连接 作为缓存
        Channel remoteChannel = HttpConnectionPool.INSTANCE.getChannel(clientId);
        if (remoteChannel != null) {
            return remoteChannel.newSucceededFuture();
        }

        return HttpClients.connect(hostAndPort, clientChannel.eventLoop())
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel proxyChannel = future.channel();

                        //https代理新建连接请求
                        if (httpRequest.method() == HttpMethod.CONNECT) {
                            clientChannel.attr(AttributeKeys.HOST_ATTRIBUTE_KEY).set(hostAndPort);
                            HttpResponse response = new DefaultFullHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK);
                            clientChannel.writeAndFlush(response);
                        }

                        //黑名单
                        if (HostFilter.filter(hostAndPort.getHost())) {
                            relay(clientChannel, proxyChannel);
                            return;
                        }

                        HttpConnectionPool.INSTANCE.add(clientId, proxyChannel);

                        //http client响应代理
                        proxyChannel.pipeline().addLast(new HttpResponseProxyHandler(clientChannel));

                        if (hostAndPort.isSsl()) {
                            //https 服务端自签证书
                            Map.Entry<PrivateKey, X509Certificate> entry = CertificateManager.getInstance().getCertificate(hostAndPort.getHost());
                            SslContext sslCtx = SslContextBuilder.forServer(entry.getKey(), entry.getValue()).build();

                            clientChannel.pipeline().addFirst(sslCtx.newHandler(clientChannel.alloc()));
                        }
                    } else {
                        log.error("建立连接失败 [{}]", hostAndPort);
                        clientChannel.close();
                    }
                });
    }

    /**
     * 转发请求
     */
    protected void forward(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        getRemoteChannel(ctx.channel(), httpRequest)
                .addListener((ChannelFutureListener) future -> {
                    Channel remoteChannel = future.channel();

                    if (httpRequest.method() != HttpMethod.CONNECT) {
                        if (ctx.channel().attr(AttributeKeys.HOST_ATTRIBUTE_KEY).get() == null) {
                            log.info("[{}] {}", ctx.channel().id(), httpRequest.uri());
                        } else {
                            log.info("[{}] {}", ctx.channel().id(), ctx.channel().attr(AttributeKeys.HOST_ATTRIBUTE_KEY).get().getHost() + httpRequest.uri());
                        }
                        //实现抓包代理转发
                        ctx.channel().attr(AttributeKeys.REQUEST_KEY).set(httpRequest);
                        remoteChannel.writeAndFlush(httpRequest);
                    }
                });
    }

    /**
     * 转发请求
     */
    private void relay(Channel clientChannel, Channel remoteChannel) {
        ChannelUtils.clearHandler(clientChannel);
        clientChannel.pipeline().addFirst(new RelayHandler(remoteChannel));

        ChannelUtils.clearHandler(remoteChannel);
        remoteChannel.pipeline().addFirst(new RelayHandler(clientChannel));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = ctx.channel().id().asShortText();

        Channel proxyChannel = HttpConnectionPool.INSTANCE.getChannel(clientId);
        if (proxyChannel != null && proxyChannel.isOpen()) {
            proxyChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("代理异常 [{}]", ctx.channel().id().asShortText(), cause);
        ctx.channel().close();
    }

}
