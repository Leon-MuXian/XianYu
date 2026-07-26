package com.serenmeet.owner.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServiceNameNormalizerTest {

    @Test
    void normalizesCompatibilityCharactersWhitespaceAndCase() {
        assertThat(ServiceNameNormalizer.normalizeDisplayName("  Ｐose　 Test  "))
                .isEqualTo("Pose Test");
        assertThat(ServiceNameNormalizer.keyOf("  Ｐose　 Test  "))
                .isEqualTo("pose test");
    }

    @Test
    void returnsEmptyValueForNull() {
        assertThat(ServiceNameNormalizer.normalizeDisplayName(null)).isEmpty();
        assertThat(ServiceNameNormalizer.keyOf(null)).isEmpty();
    }
}
