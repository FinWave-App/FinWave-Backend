package app.finwave.backend.api.files;

import app.finwave.backend.database.AbstractDatabase;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.utils.TokenGenerator;
import org.jooq.DSLContext;
import org.jooq.Record1;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;
import static org.jooq.impl.DSL.sum;

public class FilesDatabase extends AbstractDatabase {
    public FilesDatabase(DSLContext context) {
        super(context);
    }

    public Optional<FilesRecord> registerNewEmptyFile(int userId, int expiresDays, boolean isPublic, String source) {
        OffsetDateTime now = OffsetDateTime.now();

        return registerNewEmptyFile(userId, now, now.plusDays(expiresDays), isPublic, source);
    }

    public Optional<FilesRecord> registerNewEmptyFile(int userId, OffsetDateTime createdAt, OffsetDateTime expiredAt, boolean isPublic, String source) {
        String token = TokenGenerator.generateFileToken();

        return context.insertInto(FILES)
                .set(FILES.ID, token)
                .set(FILES.OWNER_ID, userId)
                .set(FILES.CREATED_AT, createdAt)
                .set(FILES.EXPIRES_AT, expiredAt)
                .set(FILES.IS_PUBLIC, isPublic)
                .set(FILES.SOURCE, source)
                .returningResult(FILES)
                .fetchOptional()
                .map(Record1::component1);
    }

    public FilesRecord updateFileInfo(String token, long size, String mimeType, String name, String description, String checksum) {
        return context.update(FILES)
                .set(FILES.SIZE, size)
                .set(FILES.MIME_TYPE, mimeType)
                .set(FILES.NAME, name)
                .set(FILES.DESCRIPTION, description)
                .set(FILES.CHECKSUM, checksum)
                .where(FILES.ID.eq(token))
                .returningResult(FILES)
                .fetchOptional()
                .map(Record1::component1)
                .orElse(null);
    }

    public Optional<FilesRecord> getFile(String token) {
        return context.selectFrom(FILES)
                .where(FILES.ID.eq(token))
                .fetchOptional();
    }

    public List<FilesRecord> getUserFiles(int userId) {
        return context.selectFrom(FILES)
                .where(FILES.OWNER_ID.eq(userId))
                .orderBy(FILES.SIZE.desc())
                .fetch();
    }

    public long userUsage(int userId) {
        return context.select(sum(FILES.SIZE))
                .from(FILES)
                .where(FILES.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .map(BigDecimal::longValue)
                .orElse(0L);
    }

    public boolean userOwnFile(int userId, String fileId) {
        return context.select(FILES.ID)
                .from(FILES)
                .where(FILES.OWNER_ID.eq(userId).and(FILES.ID.eq(fileId)))
                .fetchOptional()
                .isPresent();
    }

    public List<FilesRecord> deleteExpired(int count) {
        return context.deleteFrom(FILES)
                .where(FILES.EXPIRES_AT.lessOrEqual(OffsetDateTime.now()))
                .returningResult(FILES)
                .fetch()
                .map(Record1::component1);
    }

    public List<FilesRecord> deleteAll(int userId) {
        return context.deleteFrom(FILES)
                .where(FILES.OWNER_ID.eq(userId))
                .returningResult(FILES)
                .fetch()
                .map(Record1::component1);
    }

    public Optional<FilesRecord> deleteFile(String token) {
        return context.deleteFrom(FILES)
                .where(FILES.ID.eq(token))
                .returningResult(FILES)
                .fetchOptional()
                .map(Record1::component1);
    }
}
