package com.network.util;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wanghongen
 * 2023/5/24
 */
public class HostFilter {
    /**
     * 白名单
     */
    public static Set<String> whitelist = ConcurrentHashMap.newKeySet();

    /**
     * 黑名单
     */
    public static Set<String> blacklist = ConcurrentHashMap.newKeySet();

    static {
        buildWhitelist();
        buildBlacks();
    }

    /**
     * 构建白名单
     */
    private static void buildWhitelist() {
        ArrayList<String> whites = new ArrayList<>();
//        whites.add("*.google.com");
        whites.forEach(black -> {
            String replace = black.replace("*", ".*");
            whitelist.add(replace);
        });
    }

    /**
     * 构建黑名单
     */
    private static void buildBlacks() {
        ArrayList<String> blacks = new ArrayList<>();
        blacks.add("*.google.*");
        blacks.add("*.github.com");
        blacks.add("*.apple.*");
        blacks.add("*.qq.com");
        blacks.add("www.baidu.com");
        blacks.forEach(black -> {
            String replace = black.replace("*", ".*");
            blacklist.add(replace);
        });
    }

    public static void main(String[] args) {
        System.out.println(filter("www.google.cn"));
    }

    /**
     * 是否过滤
     */
    public static boolean filter(String host) {
        //如果白名单不为空，不在白名单里都是黑名单
        if (!whitelist.isEmpty()) {
            return whitelist.stream().noneMatch(host::matches);
        }
        if (blacklist.contains(host)) {
            return true;
        }

        return blacklist.stream().anyMatch(host::matches);
    }


}
