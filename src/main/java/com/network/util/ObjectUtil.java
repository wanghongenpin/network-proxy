package com.network.util;

import java.util.function.Supplier;

/**
 * @author wanghongen
 * 2023/5/30
 */
public interface ObjectUtil {
    static <T> T requireNonNullElseGet(T t, Supplier<T> defaultValue) {
        if (t == null) {
            t = defaultValue.get();
        }
        if (t == null) {
            throw new NullPointerException();
        }
        return t;
    }
}
