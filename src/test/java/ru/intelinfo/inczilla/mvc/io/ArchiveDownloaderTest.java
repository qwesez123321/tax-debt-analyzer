package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveDownloaderTest {

    private final ArchiveDownloader downloader = new ArchiveDownloader();

    @Test
    void downloadArchive_shouldRejectNonZipUrl_withoutNetworkCall() throws Exception {
        String result = downloader.downloadArchive("https://example.com/file.rar");
        assertNull(result);
    }
}