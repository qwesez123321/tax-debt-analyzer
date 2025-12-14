package ru.intelinfo.inczilla.mvc;

import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ZipXmlDebtParserTest {

    private final ZipXmlDebtParser parser = new ZipXmlDebtParser();

    @Test
    void processXmlFile_shouldCollectCompanyDebtCorrectly() throws XMLStreamException {
        String xml = """
                <ROOT>
                  <СведНП ИННЮЛ="1234567890">
                    <СведНедоим НаимНалог="Налог на прибыль" ОбщСумНедоим="100.50" />
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="200,25" />
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="10" />
                  </СведНП>
                </ROOT>
                """;

        Map<String, CompanyDebtInfo> companies = new HashMap<>();

        parser.processXmlFile(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                companies
        );

        assertEquals(1, companies.size());

        CompanyDebtInfo info = companies.get("1234567890");
        assertNotNull(info);

        assertEquals(new BigDecimal("310.75"), info.totalDebt); // 100.50 + 200.25 + 10
        assertEquals(new BigDecimal("100.50"), info.debtByTaxType.get("Налог на прибыль"));
        assertEquals(new BigDecimal("210.25"), info.debtByTaxType.get("НДС"));
    }

    @Test
    void processXmlFile_shouldIgnoreDebtIfInnMissing() throws XMLStreamException {
        String xml = """
                <ROOT>
                  <СведНП>
                    <СведНедоим НаимНалог="НДС" ОбщСумНедоим="10" />
                  </СведНП>
                </ROOT>
                """;

        Map<String, CompanyDebtInfo> companies = new HashMap<>();

        parser.processXmlFile(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
                companies
        );

        assertTrue(companies.isEmpty());
    }
}