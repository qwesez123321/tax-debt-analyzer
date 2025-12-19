package ru.intelinfo.inczilla.mvc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ArchiveDownloader {
    private static final Logger log = LoggerFactory.getLogger(ArchiveDownloader.class);

    public String downloadArchive(String archiveUrl) throws IOException {
        if (archiveUrl == null || !archiveUrl.toLowerCase().contains(".zip")) {
            log.error("Разрешены только .zip архивы. Получено: {}", archiveUrl);
            return null;
        }

        log.info("Скачиваем архив: {}", archiveUrl);

        HttpURLConnection conn = (HttpURLConnection) new URL(archiveUrl).openConnection();
        conn.setReadTimeout(30_000);
        conn.setConnectTimeout(30_000);

        if (conn.getResponseCode() != 200) {
            log.error("Ошибка скачивания: HTTP {}", conn.getResponseCode());
            return null;
        }

        String fileName = getFileNameFromUrl(archiveUrl);
        if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
            fileName = "archive.zip";
        }

        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(fileName)) {

            byte[] buf = new byte[8_192];
            int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);

            log.info("Архив сохранён: {}", fileName);
            return fileName;
        }
    }

    private String getFileNameFromUrl(String url) {
        int pos = url.lastIndexOf('/');
        if (pos < 0 || pos == url.length() - 1) return null;

        String name = url.substring(pos + 1);
        int q = name.indexOf('?');
        if (q > 0) name = name.substring(0, q);

        return name;
    }
}