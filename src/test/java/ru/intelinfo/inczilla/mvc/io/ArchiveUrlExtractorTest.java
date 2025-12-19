package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArchiveUrlExtractorTest {

    private final ArchiveUrlExtractor extractor = new ArchiveUrlExtractor();

    @Test
    void extractZipUrl_shouldFindZipLinkInHtml() {
        String html = """
                <html>
                  <body>
                    <a href="https://data.nalog.ru/files/debt_2024-01-01.zip">Скачать</a>
                  </body>
                </html>
                """;

        String archiveUrl = extractor.extractZipUrl(html);
        assertEquals("https://data.nalog.ru/files/debt_2024-01-01.zip", archiveUrl);
    }

    @Test
    void extractZipUrl_shouldIgnoreNonZipArchives() {
        String html = """
                <html>
                  <body>
                    <a href="https://data.nalog.ru/files/data.rar">RAR</a>
                    <a href="https://data.nalog.ru/files/data.gz">GZ</a>
                  </body>
                </html>
                """;

        assertNull(extractor.extractZipUrl(html));
    }
}