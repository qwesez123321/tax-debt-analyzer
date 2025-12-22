package ru.intelinfo.inczilla.mvc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArchiveDownloader {
    private static final Logger log = LoggerFactory.getLogger(ArchiveDownloader.class);

    /**
     * Скачивает архив по ссылке во временный файл (temp) и возвращает Path до него.
     * Важно:
     *  - скачиваем ТОЛЬКО .zip
     *  - соединение гарантированно закрывается (disconnect) в finally
     *  - файл создаётся во временной директории и должен быть удалён вызывающей стороной (обычно в finally)
     */
    public Path downloadArchiveToTemp(String archiveUrl) throws IOException {
        if (archiveUrl == null || !archiveUrl.toLowerCase().contains(".zip")) {
            log.error("Разрешены только .zip архивы. Получено: {}", archiveUrl);
            return null;
        }

        log.info("Скачиваем архив: {}", archiveUrl);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(archiveUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(30_000);

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                log.error("Ошибка скачивания: HTTP {}", code);
                return null;
            }

            Path tempZip = Files.createTempFile("taxdebt-", ".zip");

            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(tempZip)) {
                in.transferTo(out);
            }

            log.info("Архив сохранён во временный файл: {}", tempZip);
            return tempZip;

        } catch (IOException e) {
            log.error("Ошибка при скачивании архива", e);
            throw e;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}