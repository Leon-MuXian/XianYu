package com.serenmeet.auth.support;

import java.text.Normalizer;
import java.util.Locale;

/** 员工登录账号的统一规范化规则。 */
public final class StaffLoginNameNormalizer {

    private StaffLoginNameNormalizer() {
    }

    public static String normalize(String loginName) {
        if (loginName == null) {
            return "";
        }
        return Normalizer.normalize(loginName, Normalizer.Form.NFKC)
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
