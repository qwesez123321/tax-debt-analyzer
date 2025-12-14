package ru.intelinfo.inczilla.mvc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenDataArchivePageClientTest {

    private final OpenDataArchivePageClient client = new OpenDataArchivePageClient();

    @Test
    void findArchiveUrl_shouldFindZipLinkInHtml() {
        String html = """
                <html>
                  <body>
                    <a href="https://data.nalog.ru/files/debt_2024-01-01.zip">Скачать</a>
                  </body>
                </html>
                """;

        String archiveUrl = client.findArchiveUrl(html);
        assertEquals("https://data.nalog.ru/files/debt_2024-01-01.zip", archiveUrl);
    }

    @Test
    void findArchiveUrl_shouldReturnNullIfNoArchiveLink() {
        String html = """
                <html>
                  <body>
                    <a href="https://data.nalog.ru/files/readme.txt">README</a>
                  </body>
                </html>
                """;

        String archiveUrl = client.findArchiveUrl(html);
        assertNull(archiveUrl);
    }
}