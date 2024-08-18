package app.finwave.backend.utils.params.validators;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class URLValidator extends AbstractValidator<URL>{
    public URLValidator(URL raw, String name) {
        super(raw, name);
    }

    public URLValidator(URL raw) {
        super(raw, null);
    }

    public URLValidator protocolAnyMatches(String... protocols) {
        if (raw == null)
            return this;

        boolean valid = false;
        String urlProtocol = raw.getProtocol();

        for (String protocol : protocols) {
            if (!protocol.equals(urlProtocol))
                continue;

            valid = true;
            break;
        }

        if (!valid)
            invalid();

        return this;
    }

    public URLValidator notLocalAddress() {
        if (raw == null)
            return this;

        try {
            InetAddress address = InetAddress.getByName(raw.getHost());

            if (address.isSiteLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress())
                invalid();
        } catch (UnknownHostException e) {
            invalid();
        }

        return this;
    }
}
