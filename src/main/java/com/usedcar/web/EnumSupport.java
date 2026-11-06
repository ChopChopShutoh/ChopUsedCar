package com.usedcar.web;

import java.util.Locale;

/** クエリパラメータの文字列を安全に enum へ変換する */
final class EnumSupport {

    private EnumSupport() {
    }

    static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (E candidate : type.getEnumConstants()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new InvalidParameterException(type.getSimpleName() + " の値が不正です: " + value);
    }
}
