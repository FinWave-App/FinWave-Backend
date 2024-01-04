package su.knst.finwave.utils;

import java.security.SecureRandom;

public class TokenGenerator {
    protected static SecureRandom random = new SecureRandom();
    @SuppressWarnings("SpellCheckingInspection")
    protected static char[] sessionSymbols = "1234567890ABCDEFGHIKLMNOPQRSTVXYZabcdefghiklmnopqrstvxyz".toCharArray();

    public static String generateSessionToken() {
        return random
                .ints(512, 0, sessionSymbols.length)
                .mapToObj((i) -> sessionSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                .toString();
    }

    public static String generateReportToken() {
        return random
                .ints(128, 0, sessionSymbols.length)
                .mapToObj((i) -> sessionSymbols[i])
                .collect(StringBuffer::new, StringBuffer::append, StringBuffer::append)
                .toString();
    }
}
