package ru.intelinfo.inczilla.mvc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ZipXmlDebtParser {
    private static final Logger log = LoggerFactory.getLogger(ZipXmlDebtParser.class);
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    /**
     * Public API (контракт): разобрать ZIP-архив с XML-файлами ФНС и вернуть агрегацию по компаниям.
     */
    public Map<String, CompanyDebtInfo> parseArchive(String zipFilePath) throws IOException, XMLStreamException {
        Map<String, CompanyDebtInfo> result = new HashMap<>();
        log.info("Открываем архив: {}", zipFilePath);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    log.info("Чтение XML: {}", entry.getName());

                    byte[] xmlBytes = readAllBytes(zis);

                    try (InputStream xmlStream = new ByteArrayInputStream(xmlBytes)) {
                        parseSingleXml(xmlStream, result);
                    }
                }
                zis.closeEntry();
            }
        }
        return result;
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        return out.toByteArray();
    }

    /**
     * Парсинг одного XML по структуре XSD ФНС:
     * Файл/Документ/(СведНП + СведНедоим*)
     */
    private void parseSingleXml(InputStream xmlStream, Map<String, CompanyDebtInfo> map) throws XMLStreamException {
        XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(xmlStream);

        boolean insideDocument = false;
        String currentInn = null;

        while (r.hasNext()) {
            int eventType = r.next();

            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String name = r.getLocalName();

                if ("Документ".equals(name)) {
                    insideDocument = true;
                    currentInn = null;
                    continue;
                }

                if (!insideDocument) {
                    continue;
                }

                if ("СведНП".equals(name)) {
                    String inn = attr(r, "ИННЮЛ");
                    if (inn == null || inn.isBlank()) {
                        log.warn("Элемент <СведНП> без атрибута ИННЮЛ, строка {}.", safeLine(r));
                        currentInn = null;
                    } else {
                        currentInn = inn;
                        map.putIfAbsent(inn, new CompanyDebtInfo(inn));
                    }
                    continue;
                }

                if ("СведНедоим".equals(name)) {
                    if (currentInn == null) {
                        log.warn("Обнаружен <СведНедоим> без контекста ИНН (внутри Документ), строка {}.", safeLine(r));
                        continue;
                    }

                    String tax = attr(r, "НаимНалог");
                    String sum = attr(r, "ОбщСумНедоим");

                    if (tax == null || sum == null) {
                        log.warn("Элемент <СведНедоим> без НаимНалог/ОбщСумНедоим, строка {}.", safeLine(r));
                        continue;
                    }

                    BigDecimal amount;
                    try {
                        amount = parseAmount(sum);
                    } catch (NumberFormatException e) {
                        log.warn("Не удалось распарсить сумму '{}', строка {}.", sum, safeLine(r));
                        continue;
                    }

                    map.get(currentInn).addDebt(tax, amount);
                }

            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                String name = r.getLocalName();
                if ("Документ".equals(name)) {
                    insideDocument = false;
                    currentInn = null;
                }
            }
        }

    }

    private int safeLine(XMLStreamReader r) {
        try {
            if (r.getLocation() != null) return r.getLocation().getLineNumber();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private String attr(XMLStreamReader r, String name) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            if (name.equals(r.getAttributeLocalName(i))) {
                return r.getAttributeValue(i);
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String text) {
        return new BigDecimal(text.replace(",", "."));
    }
}