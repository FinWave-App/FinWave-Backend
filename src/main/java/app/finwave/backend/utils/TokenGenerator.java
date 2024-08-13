package app.finwave.backend.utils;

import java.security.SecureRandom;

public class TokenGenerator {
    protected static SecureRandom random = new SecureRandom();
    @SuppressWarnings("SpellCheckingInspection")
    protected static char[] tokenSymbols = "1234567890ABCDEFGHIKLMNOPQRSTVXYZabcdefghiklmnopqrstvxyz".toCharArray();

    public static String generateSessionToken() {
        return random
                .ints(512, 0, tokenSymbols.length)
                .mapToObj((i) -> tokenSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                .toString();
    }

    public static String generateFileToken() {
        return random
                .ints(128, 0, tokenSymbols.length)
                .mapToObj((i) -> tokenSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                .toString();
    }

    public static String generateDemoLogin() {
        return "demo_" + random
                .ints(11, 0, tokenSymbols.length)
                .mapToObj((i) -> tokenSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append);
    }

    public static String generateDemoPassword() {
        return random
                .ints(32, 0, tokenSymbols.length)
                .mapToObj((i) -> tokenSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                .toString();
    }
}
