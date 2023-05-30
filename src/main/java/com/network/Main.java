package com.network;

import com.network.proxy.ProxyServer;

/**
 * @author wanghongen
 * 2023/5/22
 */
public class Main {

    public static void main(String[] args) throws Exception {
        new ProxyServer(8888).run();
    }
}
