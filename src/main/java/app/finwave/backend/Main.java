package app.finwave.backend;

import com.google.inject.Guice;
import com.google.inject.Injector;
import app.finwave.backend.http.HttpWorker;
import app.finwave.backend.logging.LogsInitializer;
import app.finwave.backend.migration.FirstStartupInitializer;
import app.finwave.backend.service.ServicesManager;
import io.github.stefanbratanov.jvm.openai.ChatMessage;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.Tool;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class Main {
    public static final Injector INJ;

    static {
        INJ = Guice.createInjector(binder -> {

        });
    }

    public static void main(String[] args) throws IOException {
        LogsInitializer.init();

        INJ.getInstance(FirstStartupInitializer.class);
        INJ.getInstance(HttpWorker.class);
        INJ.getInstance(ServicesManager.class);
    }
}