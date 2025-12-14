package ru.intelinfo.inczilla.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class OpenDataArchivePageClient {
    private static final Logger log = LoggerFactory.getLogger(OpenDataArchivePageClient.class);

    private static final String ZIP_EXTENSION = ".zip";

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
            while (sc.hasNextLine()) sb.append(sc.nextLine()).append("\n");
            return findArchiveUrl(sb.toString());
        }
    }

    public String findArchiveUrl(String html) {
        if (html == null || html.isEmpty()) return null;

        for (String line : html.split("\n")) {
            String url = tryExtractArchiveUrl(line);
            if (url != null) return url;
        }
        return null;
    }

    private String tryExtractArchiveUrl(String line) {
        if (line == null || line.isEmpty()) return null;

        int httpIndex = line.indexOf("http");
        if (httpIndex < 0) return null;

        int end = findUrlEnd(line, httpIndex);
        if (end <= httpIndex) return null;

        String candidate = line.substring(httpIndex, end);
        return hasZipExtension(candidate) ? candidate : null;
    }

    private int findUrlEnd(String line, int start) {
        int end = line.length();
        for (char c : new char[]{'"', '\'', ' '}) {
            int idx = line.indexOf(c, start);
            if (idx > 0 && idx < end) end = idx;
        }
        return end;
    }

    private boolean hasZipExtension(String url) {
        return url != null && url.toLowerCase().contains(ZIP_EXTENSION);
    }
}