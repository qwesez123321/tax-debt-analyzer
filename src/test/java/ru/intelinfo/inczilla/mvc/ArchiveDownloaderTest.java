package ru.intelinfo.inczilla.mvc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveDownloaderTest {

    private final ArchiveDownloader downloader = new ArchiveDownloader();

    @Test
    void getFileNameFromUrl_shouldExtractNameWithoutQuery() {
        String url = "https://example.com/path/to/archive_2024.zip?param=123";
        String fileName = downloader.getFileNameFromUrl(url);
        assertEquals("archive_2024.zip", fileName);
    }

    @Test
    void getFileNameFromUrl_shouldReturnNullForBadUrl() {
        String url = "https://example.com/path/to/";
        String fileName = downloader.getFileNameFromUrl(url);
        assertNull(fileName);
    }
}