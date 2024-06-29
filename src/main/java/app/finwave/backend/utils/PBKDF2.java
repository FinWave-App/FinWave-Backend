package app.finwave.backend.utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class PBKDF2 {
    protected static SecureRandom random = new SecureRandom();

    public static boolean verify(String text, byte[] hashed, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] hashAttempt = hash(text, salt);

        return Arrays.equals(hashed, hashAttempt);
    }

    public static byte[] hash(String text, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(text.toCharArray(), salt, 8192, 1024);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");

        return f.generateSecret(spec).getEncoded();
    }

    public static boolean verify(String text, byte[] hashedWithSalt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        ByteBuffer buffer = ByteBuffer.wrap(hashedWithSalt);
        int hashSize = buffer.getInt();

        byte[] hash = new byte[hashSize];
        buffer.get(hash);

        byte[] salt = new byte[buffer.remaining()];
        buffer.get(salt);

        return verify(text, hash, salt);
    }

    public static byte[] hashWithSalt(String text) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] salt = generateSalt();
        byte[] hashed = hash(text, salt);

        ByteBuffer buffer = ByteBuffer.allocate(salt.length + hashed.length + Integer.BYTES);

        buffer.putInt(hashed.length);
        buffer.put(hashed);
        buffer.put(salt);
        buffer.flip();

        return buffer.array();
    }

    public static String hashWithSaltBase64(String text) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Base64.getEncoder().encodeToString(hashWithSalt(text));
    }

    public static boolean verifyBase64(String text, String hashedWithSalt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return verify(text, Base64.getDecoder().decode(hashedWithSalt));
    }

    public static byte[] generateSalt() {
        byte[] salt = new byte[32];
        random.nextBytes(salt);

        return salt;
    }
}