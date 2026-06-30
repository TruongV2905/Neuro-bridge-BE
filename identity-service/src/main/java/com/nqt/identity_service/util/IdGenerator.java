package com.nqt.identity_service.util;

import java.security.SecureRandom;

public final class IdGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {}

    public static String randomCode(int length) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < length; i++) {
            result.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }

        return result.toString();
    }
}
