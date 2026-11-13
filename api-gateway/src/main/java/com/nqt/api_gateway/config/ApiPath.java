package com.nqt.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiPath {

    private final String prefix;

    public ApiPath(@Value("${app.api-prefix:/api}") String configuredPrefix) {
        this.prefix = normalizePrefix(configuredPrefix);
    }

    public String withPrefix(String path) {
        String normalizedPath = path == null ? "" : path.trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath.isEmpty() ? prefix : prefix + "/" + normalizedPath;
    }

    private String normalizePrefix(String configuredPrefix) {
        String normalizedPrefix = configuredPrefix == null ? "/api" : configuredPrefix.trim();
        if (normalizedPrefix.isEmpty() || "/".equals(normalizedPrefix)) {
            return "/api";
        }
        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        while (normalizedPrefix.length() > 1 && normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }
        return normalizedPrefix;
    }
}
