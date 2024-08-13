package app.finwave.backend.api.files;

import java.io.IOException;
import java.io.OutputStream;

public interface StreamCloseListener {
    void closed(OutputStream stream) throws IOException;
}
