package app.finwave.backend.api.files;

import app.finwave.backend.jooq.tables.records.FilesRecord;
import com.google.common.io.CountingOutputStream;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;

public record FileWriteData(OutputStream os, DigestOutputStream dos, FilesRecord record) {
}
