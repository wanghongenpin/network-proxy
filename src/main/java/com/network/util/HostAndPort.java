package com.network.util;

import java.util.Objects;

/**
 * @author wanghongen
 * 2023/5/25
 */
public class HostAndPort {
    public static final String HTTP_SCHEME = "http://";
    public static final String HTTPS_SCHEME = "https://";

    private final String scheme;
    private final String host;
    private final Integer port;

    public HostAndPort(String scheme, String host, Integer port) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
    }

    /**
     * 根据url构建
     */
    public static HostAndPort of(String url) {
        String domain = url;
        String scheme = null;
        //域名格式 直接解析
        if (url.startsWith(HTTP_SCHEME)) {
            //解析域名主机和端口号
            scheme = url.startsWith(HTTPS_SCHEME) ? HTTPS_SCHEME : HTTP_SCHEME;
            domain = url.substring(scheme.length()).split("/")[0];
        }

        //ip格式 host:port
        String[] hostAndPort = domain.split(":");
        if (hostAndPort.length == 2) {
            boolean isSsl = hostAndPort[1].equals("443");
            return new HostAndPort(isSsl ? HTTPS_SCHEME : HTTP_SCHEME, hostAndPort[0], Integer.parseInt(hostAndPort[1]));
        }
        if (scheme == null) {
            scheme = HTTP_SCHEME;
        }
        return new HostAndPort(scheme, hostAndPort[0], 80);
    }

    public boolean isSsl() {
        return HTTPS_SCHEME.startsWith(scheme);
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HostAndPort)) return false;

        HostAndPort that = (HostAndPort) o;

        if (!Objects.equals(scheme, that.scheme)) return false;
        if (!Objects.equals(host, that.host)) return false;
        return Objects.equals(port, that.port);
    }

    @Override
    public int hashCode() {
        int result = scheme != null ? scheme.hashCode() : 0;
        result = 31 * result + (host != null ? host.hashCode() : 0);
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HostAndPort{" +
                "scheme='" + scheme + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
