package com.serenmeet.owner.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** 会员卡名称的统一规范化规则。 */
public final class CardTemplateNameNormalizer {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\p{Z}]+");

    private CardTemplateNameNormalizer() {
    }

    /** 保留展示大小写，同时统一兼容字符和空白。 */
    public static String normalizeDisplayName(String name) {
        if (name == null) {
            return "";
        }
        return WHITESPACE
                .matcher(Normalizer.normalize(name, Normalizer.Form.NFKC).trim())
                .replaceAll(" ");
    }

    /** 生成用于租户内唯一性比较的名称键。 */
    public static String keyOf(String name) {
        return normalizeDisplayName(name).toLowerCase(Locale.ROOT);
    }
}
