package app.finwave.backend.api.files;

import app.finwave.backend.api.ApiResponse;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.FilesConfig;
import app.finwave.backend.http.ApiMessage;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.jooq.tables.records.UsersSessionsRecord;
import app.finwave.backend.utils.params.InvalidParameterException;
import app.finwave.backend.utils.params.ParamsValidator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static spark.Spark.halt;

@Singleton
public class FilesApi {
    protected FilesManager manager;
    protected FilesConfig config;

    @Inject
    public FilesApi(FilesManager manager, Configs configs) {
        this.manager = manager;
        this.config = configs.getState(new FilesConfig());
    }

    public Object availableSpace(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");
        long max = config.maxMiBStoragePerUser * 1024 * 1024;
        long used = manager.getUserUsage(sessionRecord.getUserId());

        response.status(200);

        return new AvailableSpaceResponse(used, max - used, max);
    }

    public Object uploadFromURL(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        int expiredAfterDays = ParamsValidator
                .integer(request, "expiredAfterDays")
                .range(1, config.maxUploadedFilesExpiredDays)
                .optional()
                .orElse(config.maxUploadedFilesExpiredDays);

        boolean isPublic = ParamsValidator
                .string(request, "isPublic")
                .mapOptional(s -> s.equals("true"))
                .orElse(false);

        String mime = ParamsValidator
                .string(request, "mime")
                .length(3, 255)
                .matches((s) -> s.contains("/"))
                .require();

        String name = ParamsValidator
                .string(request, "name")
                .length(1, config.maxUploadedFilesName)
                .require();

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxUploadedFilesDescription)
                .optional()
                .orElse(null);

        URL url = ParamsValidator
                .url(request, "url")
                .protocolAnyMatches("https")
                .notLocalAddress()
                .require();

        Optional<FilesRecord> record = manager.registerNewEmptyFile(sessionRecord.getUserId(), expiredAfterDays, isPublic, "webUploadByLink");

        if (record.isEmpty())
            halt(500);

        LimitedWithCallbackOutputStream stream = null;
        try {
            stream = manager.openStream(
                    record.get(),
                    mime,
                    name,
                    description
            );
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (stream == null)
            halt(400);

        try (InputStream input = url.openStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            try {
                stream.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            if (e.getMessage().equals("Exceeded max bytes available")) {
                response.status(400);

                return ApiMessage.of("Exceeded max bytes available");
            }else {
                e.printStackTrace();
            }
        }

        try {
            stream.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        response.status(200);

        return new FileUploadResponse(record.get().getId());
    }

    public Object upload(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        int expiredAfterDays = ParamsValidator
                .integer(request, "expiredAfterDays")
                .range(1, config.maxUploadedFilesExpiredDays)
                .optional()
                .orElse(config.maxUploadedFilesExpiredDays);

        boolean isPublic = ParamsValidator
                .string(request, "isPublic")
                .mapOptional(s -> s.equals("true"))
                .orElse(false);

        String description = ParamsValidator
                .string(request, "description")
                .length(1, config.maxUploadedFilesDescription)
                .optional()
                .orElse(null);

        request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
        Part part = null;

        try {
            part = request.raw().getPart("file");
        } catch (IOException | ServletException e) {
            e.printStackTrace();

            halt(400);
        }

        Optional<FilesRecord> record = manager.registerNewEmptyFile(sessionRecord.getUserId(), expiredAfterDays, isPublic, "webUpload");

        if (record.isEmpty())
            halt(500);

        LimitedWithCallbackOutputStream stream = null;

        String fileName = part.getSubmittedFileName();
        fileName = fileName.length() > config.maxUploadedFilesName ? fileName.substring(0, config.maxUploadedFilesName) : fileName;

        try {
            stream = manager.openStream(
                    record.get(),
                    part.getContentType(),
                    fileName,
                    description
            );
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        if (stream == null)
            halt(400);

        try (InputStream input = part.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                stream.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            try {
                stream.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            if (e.getMessage().equals("Exceeded max bytes available")) {
                response.status(400);

                return ApiMessage.of("Exceeded max bytes available");
            }else {
                e.printStackTrace();
            }
        }

        try {
            stream.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        response.status(200);

        return new FileUploadResponse(record.get().getId());
    }

    public Object delete(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        String fileId = ParamsValidator
                .string(request, "fileId")
                .matches((id) -> manager.userOwnFile(sessionRecord.getUserId(), id))
                .require();

        Optional<FilesRecord> deleted = manager.delete(fileId);

        if (deleted.isEmpty())
            halt(500);

        response.status(200);

        return ApiMessage.of("File deleted");
    }

    public Object deleteAll(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        int deletedCount = manager.deleteAll(sessionRecord.getUserId()).size();

        response.status(200);

        return ApiMessage.of("Deleted " + deletedCount + " files");
    }

    public Object getList(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        List<FilesRecord> files = manager.getUserFileRecords(sessionRecord.getUserId());

        response.status(200);

        return new GetListResponse(files);
    }

    public Object download(Request request, Response response) {
        Optional<FilesRecord> record = ParamsValidator
                .string(request, "fileId")
                .map(manager::getFileRecord);

        if (record.isEmpty() || !record.get().getIsPublic()) {
            throw new InvalidParameterException("fileId");
        }

        return verifyAndSend(record.get(), response);
    }

    public Object downloadWithAuth(Request request, Response response) {
        UsersSessionsRecord sessionRecord = request.attribute("session");

        Optional<FilesRecord> record = ParamsValidator
                .string(request, "fileId")
                .matches((fileId) -> manager.userOwnFile(sessionRecord.getUserId(), fileId))
                .map(manager::getFileRecord);

        if (record.isEmpty())
            throw new InvalidParameterException("fileId");

        return verifyAndSend(record.get(), response);
    }

    protected Object verifyAndSend(FilesRecord record, Response response) {
        if (!manager.verify(record))
            throw new RuntimeException("Verification file checksum failed: does not exist or corrupted");

        Optional<File> optionalFile = manager.getFile(record);

        if (optionalFile.isEmpty())
            throw new RuntimeException("Fail getting file");

        File file = optionalFile.get();

        response.header("Content-Type", record.getMimeType());
        response.header("Content-Disposition", "attachment;filename=" + URLEncoder.encode(record.getName(), StandardCharsets.UTF_8).replaceAll("\\+", " "));

        try {
            try(BufferedOutputStream outputStream = new BufferedOutputStream(response.raw().getOutputStream());
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file)))
            {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = bufferedInputStream.read(buffer)) > 0)
                    outputStream.write(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException("Fail sending file");
        }

        response.status(200);

        return response.raw();
    }

    static class GetListResponse extends ApiResponse {
        public final List<Entry> files;

        public GetListResponse(List<FilesRecord> records) {
            this.files = records
                    .stream()
                    .map(v -> new Entry(
                            v.getId(),
                            v.getIsPublic(),
                            v.getCreatedAt(),
                            v.getExpiresAt(),
                            v.getSize() == null ? 0 : v.getSize(),
                            v.getMimeType(),
                            v.getName(),
                            v.getDescription()
                    ))
                    .toList();
        }

        record Entry(String fileId, boolean isPublic, OffsetDateTime createdAt, OffsetDateTime expiresAt, long size, String mimeType, String name, String description) {}
    }

    static class FileUploadResponse extends ApiResponse {
        public final String fileId;

        public FileUploadResponse(String fileId) {
            this.fileId = fileId;
        }
    }

    static class AvailableSpaceResponse extends ApiResponse {
        public final long usedBytes;
        public final long freeBytes;
        public final long maxBytes;

        public AvailableSpaceResponse(long usedBytes, long freeBytes, long maxBytes) {
            this.usedBytes = usedBytes;
            this.freeBytes = freeBytes;
            this.maxBytes = maxBytes;
        }
    }
}
