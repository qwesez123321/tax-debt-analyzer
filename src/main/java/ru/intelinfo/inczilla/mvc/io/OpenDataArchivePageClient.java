package ru.intelinfo.inczilla.mvc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class OpenDataArchivePageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenDataArchivePageClient.class);

    private final ArchiveUrlExtractor extractor = new ArchiveUrlExtractor();

    public String getArchiveUrl(String dataUrl) throws IOException {
        log.info("Получаем HTML-страницу: {}", dataUrl);

        HttpURLConnection connection = (HttpURLConnection) new URL(dataUrl).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        int code = connection.getResponseCode();
        log.info("Код ответа сервера: {}", code);

        if (code != HttpURLConnection.HTTP_OK) {
            log.warn("Невозможно получить страницу: {}", code);
            return null;
        }

        try (Scanner sc = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            while (sc.hasNextLine()) sb.append(sc.nextLine()).append('\n');
            return extractor.extractZipUrl(sb.toString());
        }
    }


}