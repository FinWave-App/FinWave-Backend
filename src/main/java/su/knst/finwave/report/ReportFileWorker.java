package su.knst.finwave.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ReportFileWorker {
    public static final Path reportsPath = Path.of("./reports/");

    public static File create(String token) throws IOException {
        File target = getFile(token);

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

    public static Optional<File> get(String token) {
        File target = getFile(token);

        return Optional.ofNullable(target.exists() && target.isFile() ? target : null);
    }

    public static void delete(String token) throws IOException {
        Optional<File> file = get(token);

        if (file.isPresent())
            Files.delete(file.get().toPath());
    }

    protected static File getFile(String token) {
        return reportsPath.resolve(token.charAt(0) + "/" + token).toFile();
    }
}
