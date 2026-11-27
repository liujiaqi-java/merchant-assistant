package com.merchantassistant.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

final class PasswordUtil {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    private PasswordUtil() { }
    static String hash(String password) { return BCRYPT.encode(password); }
    static boolean matches(String password, String hash) {
        return hash != null && (hash.startsWith("$2") ? BCRYPT.matches(password, hash) : MessageDigest.isEqual(legacyHash(password).getBytes(StandardCharsets.UTF_8), hash.getBytes(StandardCharsets.UTF_8)));
    }
    static boolean needsUpgrade(String hash) { return hash == null || !hash.startsWith("$2"); }
    private static String legacyHash(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) { throw new IllegalStateException("密码加密失败", exception); }
    }
}
