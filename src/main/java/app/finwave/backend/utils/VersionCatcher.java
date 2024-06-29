package app.finwave.backend.utils;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

public class VersionCatcher {
    public static final String VERSION;

    static {
        ClassLoader cl = VersionCatcher.class.getClassLoader();
        URL url = cl.getResource("META-INF/MANIFEST.MF");
        Manifest manifest;
        try {
            assert url != null;
            manifest = new Manifest(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        String manifestVersion = manifest.getMainAttributes().getValue("Implementation-Version");

        VERSION = manifestVersion == null ? "dev" : manifestVersion;
    }
}
