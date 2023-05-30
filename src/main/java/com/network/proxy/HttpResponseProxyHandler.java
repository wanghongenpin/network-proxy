package com.network.proxy;

import com.network.client.HttpConnectionPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wanghongen
 * 2023/5/23
 */
public class HttpResponseProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(HttpResponseProxyHandler.class);

    /**
     * 排除的后缀 不打印日志
     */
    private static final Set<String> excludeContent = new HashSet<>(Arrays.asList("javascript", "text/css", "application/font-woff", "image"));

    private final Channel clientChannel;

    public HttpResponseProxyHandler(Channel channel) {
        this.clientChannel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof HttpResponse)) {
            super.channelRead(ctx, msg);
            return;
        }
        String contentType = ((HttpResponse) msg).headers().getAsString(HttpHeaderNames.CONTENT_TYPE);
        if (contentType != null && excludeContent.stream().noneMatch(contentType::contains)) {
            if (msg instanceof HttpContent) {
                log.info("[{}] Response {}", clientChannel.id(), ((HttpContent) msg).content().toString(Charset.defaultCharset()));
            } else {
                log.info("[{}] 接收到远程的数据 " + msg, clientChannel.id());
            }
        }

        //发送给客户端
        clientChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String clientId = clientChannel.id().asShortText();
        HttpConnectionPool.INSTANCE.remove(clientId);

        //关闭客户端连接
        if (clientChannel.isOpen()) {
            clientChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("http response proxy handler error", cause);
    }
}
