package app.finwave.backend.api.account.folder;

import app.finwave.backend.jooq.tables.records.AccountsFoldersRecord;
import org.jooq.*;
import app.finwave.backend.database.AbstractDatabase;

import java.util.List;
import java.util.Optional;

import static app.finwave.backend.jooq.Tables.*;

public class AccountFolderDatabase extends AbstractDatabase {

    public AccountFolderDatabase(DSLContext context) {
        super(context);
    }

    public Optional<Long> newFolder(int userId, String name, String description) {
        return context.insertInto(ACCOUNTS_FOLDERS)
                .set(ACCOUNTS_FOLDERS.OWNER_ID, userId)
                .set(ACCOUNTS_FOLDERS.NAME, name)
                .set(ACCOUNTS_FOLDERS.DESCRIPTION, description)
                .returningResult(ACCOUNTS_FOLDERS.ID)
                .fetchOptional()
                .map(Record1::component1);
    }

    public Optional<AccountsFoldersRecord> getFolder(long id) {
        return context.selectFrom(ACCOUNTS_FOLDERS)
                .where(ACCOUNTS_FOLDERS.ID.eq(id))
                .fetchOptional();
    }

    public List<AccountsFoldersRecord> getFolders(int userId) {
        return context.selectFrom(ACCOUNTS_FOLDERS)
                .where(ACCOUNTS_FOLDERS.OWNER_ID.eq(userId))
                .orderBy(ACCOUNTS_FOLDERS.ID)
                .fetch();
    }

    public void editFolderName(long id, String name) {
        context.update(ACCOUNTS_FOLDERS)
                .set(ACCOUNTS_FOLDERS.NAME, name)
                .where(ACCOUNTS_FOLDERS.ID.eq(id))
                .execute();
    }

    public void editFolderDescription(long id, String description) {
        context.update(ACCOUNTS_FOLDERS)
                .set(ACCOUNTS_FOLDERS.DESCRIPTION, description)
                .where(ACCOUNTS_FOLDERS.ID.eq(id))
                .execute();
    }

    public boolean userOwnFolder(int userId, long folderId) {
        return context.select(ACCOUNTS_FOLDERS.ID)
                .from(ACCOUNTS_FOLDERS)
                .where(ACCOUNTS_FOLDERS.OWNER_ID.eq(userId).and(ACCOUNTS_FOLDERS.ID.eq(folderId)))
                .fetchOptional()
                .isPresent();
    }

    public int getFolderCount(int userId) {
        return context.selectCount()
                .from(ACCOUNTS_FOLDERS)
                .where(ACCOUNTS_FOLDERS.OWNER_ID.eq(userId))
                .fetchOptional()
                .map(Record1::component1)
                .orElse(0);
    }

    public boolean folderSafeToDelete(long folderId) {
        return context.selectCount()
                .from(ACCOUNTS)
                .where(ACCOUNTS.FOLDER_ID.eq(folderId))
                .fetchOptional()
                .map(Record1::component1)
                .map(c -> c == 0)
                .orElse(false);
    }

    public void deleteFolder(long folderId) {
        context.deleteFrom(ACCOUNTS_FOLDERS)
                .where(ACCOUNTS_FOLDERS.ID.eq(folderId))
                .execute();
    }
}
