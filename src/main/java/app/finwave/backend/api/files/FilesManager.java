package app.finwave.backend.api.files;

import app.finwave.backend.api.event.WebSocketWorker;
import app.finwave.backend.api.event.messages.response.NotifyUpdate;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.CachingConfig;
import app.finwave.backend.config.general.FilesConfig;
import app.finwave.backend.database.DatabaseWorker;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.utils.CacheHandyBuilder;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Singleton
public class FilesManager {
    protected FilesDatabase database;
    protected CachingConfig cachingConfig;
    protected FilesConfig config;

    protected LoadingCache<String, Optional<FilesRecord>> fileCache;
    protected LoadingCache<Integer, ArrayList<FilesRecord>> userFilesCache;
    protected LoadingCache<Integer, Long> userUsageCache;

    protected ConcurrentHashMap<LimitedWithCallbackOutputStream, FileWriteData> openStreams = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Integer, ReentrantLock> userWriteLocks = new ConcurrentHashMap<>();

    public static final Path filesPath = Path.of("./files/");

    protected WebSocketWorker socketWorker;

    protected ArrayList<Consumer<FilesRecord>> fileDeletionListeners = new ArrayList<>();

    @Inject
    public FilesManager(DatabaseWorker worker, Configs configs, WebSocketWorker socketWorker) {
        this.database = worker.get(FilesDatabase.class);
        this.cachingConfig = configs.getState(new CachingConfig());
        this.config = configs.getState(new FilesConfig());

        this.socketWorker = socketWorker;

        this.fileCache = CacheHandyBuilder.loading(
                7, TimeUnit.DAYS,
                cachingConfig.files.maxFiles,
                database::getFile
        );

        this.userUsageCache = CacheHandyBuilder.loading(
                7, TimeUnit.DAYS,
                cachingConfig.files.maxUsages,
                database::userUsage
        );

        this.userFilesCache = CacheHandyBuilder.loading(
                7, TimeUnit.DAYS,
                cachingConfig.files.maxLists,
                (userId) -> new ArrayList<>(database.getUserFiles(userId)),
                (e) -> userUsageCache.invalidate(e.getKey())
        );
    }

    public boolean userOwnFile(int userId, String fileId) {
        var list = userFilesCache.getIfPresent(userId);

        if (list == null)
            return database.userOwnFile(userId, fileId);

        for (var entry : list) {
            if (entry.getId().equals(fileId))
                return true;
        }

        return false;
    }

    public long getUserUsage(int userId) {
        try {
            return userUsageCache.get(userId);
        } catch (ExecutionException e) {
            e.printStackTrace();

            return 0;
        }
    }

    public void addFileDeletionListener(Consumer<FilesRecord> listener) {
        fileDeletionListeners.add(listener);
    }

    public Optional<OutputStream> getAndOpenStream(String token, String mimeType, String name, String description) {
        Optional<FilesRecord> filesRecord = getFileRecord(token);

        if (filesRecord.isEmpty())
            return Optional.empty();

        try {
            return Optional.of(openStream(filesRecord.get(), mimeType, name, description));
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();

            return Optional.empty();
        }
    }

    public Optional<LimitedWithCallbackOutputStream> registerAndOpenStream(int userId, OffsetDateTime createdAt,
                                                                           OffsetDateTime expiresAt, boolean isPublic,
                                                                           String source, String mimeType, String name,
                                                                           String description) {
        Optional<FilesRecord> filesRecord = registerNewEmptyFile(userId, createdAt, expiresAt, isPublic, source);

        if (filesRecord.isEmpty())
            return Optional.empty();

        try {
            return Optional.of(openStream(filesRecord.get(), mimeType, name, description));
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();

            return Optional.empty();
        }
    }

    public Optional<LimitedWithCallbackOutputStream> registerAndOpenStream(int userId, int expiresDays, boolean isPublic,
                                                                           String source, String mimeType, String name,
                                                                           String description) {
        OffsetDateTime now = OffsetDateTime.now();

        return registerAndOpenStream(userId, now, now.plusDays(expiresDays), isPublic, source, mimeType, name, description);
    }

    public LimitedWithCallbackOutputStream openStream(FilesRecord record, String mimeType, String name, String description) throws IOException, NoSuchAlgorithmException {
        ReentrantLock userLock = userWriteLocks.computeIfAbsent(record.getOwnerId(), k -> new ReentrantLock());

        userLock.lock();

        long bytesAvailable = config.maxMiBStoragePerUser * 1024 * 1024 - getUserUsage(record.getOwnerId());

        if (bytesAvailable <= 0) {
            userLock.unlock();

            throw new IOException("Not enough storage space available");
        }

        LimitedWithCallbackOutputStream limitedOutputStream;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            File file = getOrCreateFile(record);

            OutputStream os = new FileOutputStream(file);
            DigestOutputStream dos = new DigestOutputStream(os, md);
            limitedOutputStream = new LimitedWithCallbackOutputStream(
                    dos, bytesAvailable,
                    (s) -> {
                        try {
                            saveStream((LimitedWithCallbackOutputStream) s, mimeType, name, description);
                        }finally {
                            userLock.unlock();
                        }
                    }
            );

            openStreams.put(limitedOutputStream, new FileWriteData(os, dos, record));
        }catch (Exception e) {
            userLock.unlock();

            throw e;
        }

        return limitedOutputStream;
    }

    public Optional<FilesRecord> getRecordFromStream(LimitedWithCallbackOutputStream stream) {
        return Optional.ofNullable(openStreams.get(stream)).map(FileWriteData::record);
    }

    protected void saveStream(LimitedWithCallbackOutputStream stream, String mimeType, String name, String description) throws IllegalArgumentException, IOException {
        FileWriteData fileWriteData;

        fileWriteData = openStreams.remove(stream);

        if (fileWriteData == null)
            throw new IllegalArgumentException("Stream does not exist");

        StringBuilder sb = new StringBuilder();

        byte[] bytes = fileWriteData.dos().getMessageDigest().digest();

        for (byte b : bytes)
            sb.append(String.format("%02x", b));

        updateFileInfo(fileWriteData.record().getId(), stream.getBytesWritten(), mimeType, name, description, sb.toString());
    }

    public Optional<FilesRecord> registerNewEmptyFile(int userId, OffsetDateTime createdAt, OffsetDateTime expiresAt, boolean isPublic, String source) {
        var result = database.registerNewEmptyFile(userId, createdAt, expiresAt, isPublic, source);

        if (result.isEmpty())
            return Optional.empty();

        fileCache.put(result.get().getId(), result);

        ArrayList<FilesRecord> userList = userFilesCache.getIfPresent(userId);

        if (userList != null)
            userList.add(result.get());

        socketWorker.sendToUser(userId, new NotifyUpdate("files"));

        return result;
    }

    public Optional<FilesRecord> registerNewEmptyFile(int userId, int expiresDays, boolean isPublic, String source) {
        OffsetDateTime now = OffsetDateTime.now();

        return registerNewEmptyFile(userId, now, now.plusDays(expiresDays), isPublic, source);
    }

    public FilesRecord updateFileInfo(String token, long size, String mimeType, String name, String description, String checksum) {
        FilesRecord record = database.updateFileInfo(token, size, mimeType, name, description, checksum);

        if (record != null) {
            fileCache.put(record.getId(), Optional.of(record));
            userFilesCache.invalidate(record.getOwnerId());

            socketWorker.sendToUser(record.getOwnerId(), new NotifyUpdate("files"));
        }

        return record;
    }

    public Optional<FilesRecord> getFileRecord(String token) {
        try {
            return fileCache.get(token);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<FilesRecord> getUserFileRecords(int userId) {
        try {
            return userFilesCache.get(userId);
        } catch (ExecutionException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public Optional<FilesRecord> delete(String token) {
        Optional<FilesRecord> deletedFile = database.deleteFile(token);

        deletedFile.ifPresent(record -> {
            fileCache.invalidate(record.getId());
            userFilesCache.invalidate(record.getOwnerId());

            try {
                deleteFile(record);
            } catch (IOException e) {
                e.printStackTrace();
            }

            fileDeletionListeners.forEach((l) -> l.accept(record));
            socketWorker.sendToUser(record.getOwnerId(), new NotifyUpdate("files"));
        });

        return deletedFile;
    }

    public List<FilesRecord> deleteAll(int userId) {
        List<FilesRecord> deletedFiles = database.deleteAll(userId);

        for (FilesRecord record : deletedFiles) {
            try {
                deleteFile(record);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileCache.invalidateAll(deletedFiles
                .stream()
                .map(FilesRecord::getId)
                .toList()
        );

        userFilesCache.invalidate(userId);

        deletedFiles.forEach((r) ->
                fileDeletionListeners.forEach((l) -> l.accept(r))
        );

        socketWorker.sendToUser(userId, new NotifyUpdate("files"));

        return deletedFiles;
    }

    public List<FilesRecord> deleteExpired(int count) {
        List<FilesRecord> deletedFiles = database.deleteExpired(count);

        HashSet<Integer> usersAffected = new HashSet<>();

        for (FilesRecord record : deletedFiles) {
            try {
                deleteFile(record);
                usersAffected.add(record.getOwnerId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        fileCache.invalidateAll(deletedFiles
                .stream()
                .map(FilesRecord::getId)
                .toList()
        );

        userFilesCache.invalidateAll(usersAffected);

        deletedFiles.forEach((r) ->
                fileDeletionListeners.forEach((l) -> l.accept(r))
        );

        usersAffected.forEach((userId) -> {
            socketWorker.sendToUser(userId, new NotifyUpdate("files"));
        });

        return deletedFiles;
    }

    public Optional<File> getFile(FilesRecord fileRecord) {
        File target = getFile(fileRecord.getId());

        return Optional.ofNullable(target.exists() && target.isFile() ? target : null);
    }

    private File getOrCreateFile(FilesRecord fileRecord) throws IOException {
        File target = getFile(fileRecord.getId());

        if (!target.exists()) {
            File parent = target.getParentFile();
            if (parent != null)
                target.getParentFile().mkdirs();

            target.createNewFile();
        }

        if (!target.isFile()) {
            throw new FileNotFoundException();
        }

        return target;
    }

    public boolean verify(FilesRecord fileRecord) {
        Optional<File> fileOptional = getFile(fileRecord);

        if (fileOptional.isEmpty())
            return false;

        try {
            return getFileChecksum(fileOptional.get()).equals(fileRecord.getChecksum());
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();

            return false;
        }
    }

    private void deleteFile(FilesRecord fileRecord) throws IOException {
        Optional<File> file = getFile(fileRecord);

        if (file.isPresent())
            Files.delete(file.get().toPath());
    }

    private File getFile(String token) {
        return filesPath.resolve(token.charAt(0) + "/" + token).toFile();
    }

    private String getFileChecksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes)
            sb.append(String.format("%02x", b));

        return sb.toString();
    }
}