package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipXmlDebtParserTest {

    private final ZipXmlDebtParser parser = new ZipXmlDebtParser();

    @Test
    void parseArchive_shouldCollectCompanyDebtCorrectly_withFnsStructure() throws Exception {
        String xml = """
                <Файл ИдФайл="test.xml" ВерсФорм="4.01" ТипИнф="ОТКРДАННЫЕ6" КолДок="1">
                  <Документ ИдДок="1" ДатаДок="01.01.2025" ДатаСост="01.01.2025">
                    <СведНП ИННЮЛ="1234567890" НаимОрг="ООО РОМАШКА"/>
                    <СведНедоим НаимНалог="Налог на прибыль" ОбщСумНедоим="100.50"/>
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="200.25"/>
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="10.00"/>
                  </Документ>
                </Файл>
                """;

        Path zip = createZipWithSingleXml(xml);
        Map<String, CompanyDebtInfo> companies = parser.parseArchive(zip.toString());

        assertEquals(1, companies.size());

        CompanyDebtInfo info = companies.get("1234567890");
        assertNotNull(info);

        assertEquals(new BigDecimal("310.75"), info.totalDebt);
        assertEquals(new BigDecimal("100.50"), info.debtByTaxType.get("Налог на прибыль"));
        assertEquals(new BigDecimal("210.25"), info.debtByTaxType.get("НДС"));
    }

    @Test
    void parseArchive_shouldNotCarryInnAcrossDocuments() throws Exception {
        String xml = """
                <Файл ИдФайл="test.xml" ВерсФорм="4.01" ТипИнф="ОТКРДАННЫЕ6" КолДок="2">
                  <Документ ИдДок="1" ДатаДок="01.01.2025" ДатаСост="01.01.2025">
                    <СведНП ИННЮЛ="1111111111" НаимОрг="ORG1"/>
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="1.00"/>
                  </Документ>
                  <Документ ИдДок="2" ДатаДок="01.01.2025" ДатаСост="01.01.2025">
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="999.00"/>
                  </Документ>
                </Файл>
                """;

        Path zip = createZipWithSingleXml(xml);
        Map<String, CompanyDebtInfo> companies = parser.parseArchive(zip.toString());

        CompanyDebtInfo c1 = companies.get("1111111111");
        assertNotNull(c1);
        assertEquals(new BigDecimal("1.00"), c1.totalDebt);

        // Вторая запись не должна попасть к первой компании
        assertEquals(1, companies.size());
    }

    private Path createZipWithSingleXml(String xml) throws Exception {
        Path zip = Files.createTempFile("taxdebt-", ".zip");
        zip.toFile().deleteOnExit();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
            zos.putNextEntry(new ZipEntry("data.xml"));
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return zip;
    }
}