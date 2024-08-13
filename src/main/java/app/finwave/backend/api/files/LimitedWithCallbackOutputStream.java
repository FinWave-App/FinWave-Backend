package app.finwave.backend.api.files;

import app.finwave.backend.jooq.tables.records.FilesRecord;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class LimitedWithCallbackOutputStream extends FilterOutputStream {
    private long bytesAvailable;
    private long bytesWritten;
    private final StreamCloseListener listener;

    public LimitedWithCallbackOutputStream(OutputStream out, long maxBytes, StreamCloseListener listener) {
        super(out);

        this.bytesAvailable = maxBytes;
        this.listener = listener;
    }

    public long getBytesAvailable() {
        return bytesAvailable;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    private void checkAvailable(int nextBytesCount) throws IOException {
        if (bytesAvailable - nextBytesCount < 0)
            throw new IOException("Exceeded max bytes available");
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkAvailable(len);

        out.write(b, off, len);
        bytesWritten += len;
        bytesAvailable -= len;
    }

    @Override
    public void write(int b) throws IOException {
        checkAvailable(1);

        out.write(b);
        bytesWritten++;
        bytesAvailable--;
    }

    @Override
    public void close() throws IOException {
        out.close();

        listener.closed(this);
    }
}
