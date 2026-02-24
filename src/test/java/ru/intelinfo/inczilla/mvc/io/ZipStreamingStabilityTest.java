package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipStreamingStabilityTest {

    @Test
    void shouldParseMultipleXmlEntriesWithoutStreamClosedError() throws Exception {

        File tempZip = File.createTempFile("test", ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip))) {

            String xml = """
                    <Файл>
                        <Документ>
                            <СведНП ИННЮЛ="1234567890"/>
                            <СведНедоим НаимНалог="Налог" ОбщСумНедоим="100.00"/>
                        </Документ>
                    </Файл>
                    """;

            for (int i = 0; i < 3; i++) {
                zos.putNextEntry(new ZipEntry("file" + i + ".xml"));
                zos.write(xml.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        ZipXmlDebtParser parser = new ZipXmlDebtParser();
        AtomicInteger counter = new AtomicInteger();

        parser.parseArchiveStreaming(tempZip.getAbsolutePath(),
                (inn, tax, amount) -> {
                    assertEquals("1234567890", inn);
                    assertEquals("Налог", tax);
                    assertEquals(new BigDecimal("100.00"), amount);
                    counter.incrementAndGet();
                });

        assertEquals(3, counter.get());
    }
}