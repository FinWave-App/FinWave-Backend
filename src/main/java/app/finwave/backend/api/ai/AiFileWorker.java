package app.finwave.backend.api.ai;

import app.finwave.backend.api.files.FilesManager;
import app.finwave.backend.api.files.LimitedWithCallbackOutputStream;
import app.finwave.backend.config.Configs;
import app.finwave.backend.config.general.AiConfig;
import app.finwave.backend.jooq.tables.records.FilesRecord;
import app.finwave.backend.utils.VersionCatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.stefanbratanov.jvm.openai.ContentPart;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Singleton
public class AiFileWorker {
    protected FilesManager filesManager;

    protected String filesApiRoute = System.getenv("API_URL") + "files/download";
    protected AiConfig config;
    protected AiWorker aiWorker;

    @Inject
    public AiFileWorker(FilesManager filesManager, AiWorker aiWorker, Configs configs) {
        this.filesManager = filesManager;
        this.config = configs.getState(new AiConfig());
        this.aiWorker = aiWorker;
    }

    public boolean attachFiles(long contextId, List<FilesRecord> files) {
        boolean result = false;

        for (FilesRecord record : files) {
            String mime = record.getMimeType();

            boolean notExactly = false;

            result = switch (mime) {
                case "image/jpeg", "image/png", "image/gif", "image/webp" -> attachImages(List.of(record), contextId);
                case "application/pdf" -> attachPDFs(List.of(record), contextId);
                default -> {
                    notExactly = true;

                    yield false;
                }
            };

            if (notExactly) {
                String geneticType = mime.split("/")[0];

                if (geneticType.equals("text"))
                    result = attachTexts(List.of(record), contextId);
            }

            if (!result)
                return false;
        }

        return result;
    }

    protected boolean sizeValid(List<FilesRecord> files) {
        long sizeSum = 0;

        for (FilesRecord file : files) {
            sizeSum += file.getSize();

            if (sizeSum > config.maxFilesSizeSumPerAttachmentKiB * 1024L)
                return false;
        }

        return true;
    }

    protected boolean attachPDFs(List<FilesRecord> files, long contextId) {
        ArrayList<FilesRecord> images = new ArrayList<>();
        ArrayList<FilesRecord> texts = new ArrayList<>();

        for (FilesRecord record : files) {
            Optional<File> optionalFile = filesManager.getFile(record);

            if (optionalFile.isEmpty())
                return false;

            try (PDDocument document = Loader.loadPDF(optionalFile.get())) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                PDFRenderer renderer = new PDFRenderer(document);

                for (int page = 0; page < document.getNumberOfPages(); ++page) {
                    BufferedImage image = renderer.renderImageWithDPI(page, 300, ImageType.RGB);

                    Optional<LimitedWithCallbackOutputStream> streamOptional = filesManager.registerAndOpenStream(record.getOwnerId(),
                            record.getCreatedAt(), record.getExpiresAt(), record.getIsPublic(),
                            "aiFileConverter", "image/png", record.getName() + " #" + (page + 1), record.getDescription()
                    );

                    if (streamOptional.isEmpty())
                        return false;

                    LimitedWithCallbackOutputStream stream = streamOptional.get();
                    ImageIO.write(image, "PNG", stream);

                    Optional<String> fileId = filesManager.getRecordFromStream(stream).map(FilesRecord::getId); // get only fileId, because after close stream record change
                    stream.close();

                    if (fileId.isEmpty())
                        return false;

                    images.add(filesManager.getFileRecord(fileId.get()).orElseThrow());
                }

                String text = pdfStripper.getText(document);

                if (text != null && !text.isBlank()) {
                    Optional<LimitedWithCallbackOutputStream> streamOptional = filesManager.registerAndOpenStream(record.getOwnerId(),
                            record.getCreatedAt(), record.getExpiresAt(), record.getIsPublic(),
                            "aiFileConverter", "text/plain", record.getName() + " [plain text]", record.getDescription()
                    );

                    if (streamOptional.isEmpty())
                        return false;

                    LimitedWithCallbackOutputStream stream = streamOptional.get();

                    Writer writer = new OutputStreamWriter(stream);
                    BufferedWriter bufferedWriter = new BufferedWriter(writer);

                    bufferedWriter.write(text);

                    Optional<String> fileId = filesManager.getRecordFromStream(stream).map(FilesRecord::getId);  // get only fileId, because after close stream record change

                    bufferedWriter.flush();
                    bufferedWriter.close();

                    if (fileId.isEmpty())
                        return false;

                    texts.add(filesManager.getFileRecord(fileId.get()).orElseThrow());
                }

            } catch (IOException e) {
                e.printStackTrace();

                return false;
            }
        }

        boolean result = attachImages(images, contextId);

        if (!result)
            return false;

        result = attachTexts(texts, contextId);

        return result;
    }

    protected boolean attachTexts(List<FilesRecord> files, long contextId) {
        if (!sizeValid(files))
            return false;

        for (FilesRecord record : files) {
            Optional<File> fileOptional = filesManager.getFile(record);

            if (fileOptional.isEmpty())
                return false;

            String data;

            try {
                data = config.fileAttachmentTip.replace("{_CONTENT_}", Files.readString(fileOptional.get().toPath()));
            } catch (IOException e) {
                return false;
            }

            boolean result = aiWorker.pushMessage(contextId, "system", List.of(
                    ContentPart.textContentPart(data)
            ));

            if (!result)
                return false;
        }

        return true;
    }

    protected boolean attachImages(List<FilesRecord> files, long contextId) {
        if (!sizeValid(files))
            return false;

        List<ContentPart> parts;

        if (!VersionCatcher.VERSION.equals("dev")) {
            parts = files
                    .stream()
                    .map((r) -> filesApiRoute + "?fileId=" + r.getId())
                    .map(ContentPart::imageUrlContentPart)
                    .map((i) -> (ContentPart) i)
                    .toList();
        }else { // usually the dev environment is not accessible from outside to download the file by URL, so we pass the base64 text
            parts = new ArrayList<>();

            for (FilesRecord record : files) {
                Optional<File> fileOptional = filesManager.getFile(record);

                if (fileOptional.isEmpty())
                    continue;

                String baseString;

                try {
                    baseString = Base64.getEncoder().encodeToString(Files.readAllBytes(fileOptional.get().toPath()));
                } catch (IOException e) {
                    e.printStackTrace();

                    continue;
                }

                parts.add(ContentPart.imageUrlContentPart("data:" + record.getMimeType() + ";base64," + baseString));
            }
        }

        return aiWorker.pushMessage(contextId, "user", parts);
    }
}
