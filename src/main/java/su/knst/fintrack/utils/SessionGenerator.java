package su.knst.fintrack.utils;

import java.security.SecureRandom;

public class SessionGenerator {
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
}
