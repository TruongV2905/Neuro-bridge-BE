package com.nqt.api_gateway.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiPathTest {

    @Test
    void joinsThePrefixAndPathWithExactlyOneSlash() {
        ApiPath apiPath = new ApiPath("/api/");

        assertThat(apiPath.withPrefix("/identity/auth/**")).isEqualTo("/api/identity/auth/**");
        assertThat(apiPath.withPrefix("ai-available/v1/ai-place/expert/products"))
                .isEqualTo("/api/ai-available/v1/ai-place/expert/products");
    }

    @Test
    void normalizesPrefixOverridesThatContainExtraTrailingSlashes() {
        ApiPath apiPath = new ApiPath("api///");

        assertThat(apiPath.withPrefix("/payment/**")).isEqualTo("/api/payment/**");
    }
}
