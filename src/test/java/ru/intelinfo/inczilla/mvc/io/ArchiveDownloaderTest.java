package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveDownloaderTest {

    private final ArchiveDownloader downloader = new ArchiveDownloader();

    @Test
    void downloadArchiveToTemp_shouldRejectNullUrl() throws Exception {
        Path result = downloader.downloadArchiveToTemp(null);
        assertNull(result);
    }

    @Test
    void downloadArchiveToTemp_shouldRejectNonZipUrl_withoutNetworkCall() throws Exception {
        Path result = downloader.downloadArchiveToTemp("https://example.com/file.rar");
        assertNull(result);
    }

    @Test
    void downloadArchiveToTemp_shouldRejectUrlWithoutZipExtension_withoutNetworkCall() throws Exception {
        Path result = downloader.downloadArchiveToTemp("https://example.com/file");
        assertNull(result);
    }
}