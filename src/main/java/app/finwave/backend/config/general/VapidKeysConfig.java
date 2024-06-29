package app.finwave.backend.config.general;

import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import app.finwave.backend.config.ConfigGroup;
import app.finwave.backend.config.GroupedConfig;
import app.finwave.backend.utils.VapidGenerator;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class VapidKeysConfig implements GroupedConfig {
    protected static final String publicGenerated;
    protected static final String privateGenerated;

    static {
        KeyPair pair;

        try {
            pair = VapidGenerator.generate();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }

        publicGenerated = VapidGenerator.keyToString((ECPublicKey) pair.getPublic());
        privateGenerated = VapidGenerator.keyToString((ECPrivateKey) pair.getPrivate());
    }

    public String publicKey = publicGenerated;
    public String privateKey = privateGenerated;

    @Override
    public ConfigGroup group() {
        return ConfigGroup.GENERAL;
    }
}
