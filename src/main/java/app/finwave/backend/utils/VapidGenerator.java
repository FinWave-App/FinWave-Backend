package app.finwave.backend.utils;

import com.google.common.io.BaseEncoding;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

public class VapidGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair generate() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        ECNamedCurveParameterSpec parameterSpec =
                ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);

        return keyPairGenerator.generateKeyPair();
    }

    public static ECPublicKey stringToPublicKey(String key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        byte[] bytes = BaseEncoding.base64Url().decode(key);

        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
        ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        ECCurve curve = parameterSpec.getCurve();
        ECPoint point = curve.decodePoint(bytes);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, parameterSpec);

        return (ECPublicKey) keyFactory.generatePublic(pubSpec);
    }

    public static ECPrivateKey stringToPrivateKey(String key) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
        byte[] bytes = BaseEncoding.base64Url().decode(key);

        BigInteger s = BigIntegers.fromUnsignedByteArray(bytes);
        ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(s, parameterSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");

        return (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);
    }

    public static String keyToString(ECPublicKey key) {
        return BaseEncoding.base64Url().encode(Utils.encode(key));
    }

    public static String keyToString(ECPrivateKey key) {
        return BaseEncoding.base64Url().encode(Utils.encode(key));
    }
}
