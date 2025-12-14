package ru.intelinfo.inczilla.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipXmlDebtParser {
    private static final Logger log = LoggerFactory.getLogger(ZipXmlDebtParser.class);
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    public Map<String, CompanyDebtInfo> processArchive(String archive)
            throws IOException, XMLStreamException {

        Map<String, CompanyDebtInfo> map = new HashMap<>();
        log.info("Открываем архив: {}", archive);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".xml")) {

                    log.info("Чтение XML: {}", entry.getName());

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = zis.read(buf)) > 0) baos.write(buf, 0, r);

                    processXmlFile(
                            new ByteArrayInputStream(baos.toByteArray()),
                            map
                    );
                }
                zis.closeEntry();
            }
        }

        return map;
    }

    public void processXmlFile(InputStream xml, Map<String, CompanyDebtInfo> map)
            throws XMLStreamException {

        XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(xml);
        String currentInn = null;

        while (r.hasNext()) {
            int eventType = r.next();

            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String name = r.getLocalName();

                if ("СведНП".equals(name)) {
                    String inn = attr(r, "ИННЮЛ");
                    currentInn = inn;

                    if (inn == null || inn.isEmpty()) {
                        log.warn("Элемент <СведНП> без атрибута ИННЮЛ, строка {}.", safeLine(r));
                        currentInn = null;
                    } else {
                        map.putIfAbsent(inn, new CompanyDebtInfo(inn));
                    }
                } else if ("СведНедоим".equals(name)) {

                    if (currentInn == null) {
                        log.warn("Обнаружен <СведНедоим> вне <СведНП>, строка {}.", safeLine(r));
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
            }
        }

        r.close();
    }

    private int safeLine(XMLStreamReader r) {
        try {
            if (r.getLocation() != null) return r.getLocation().getLineNumber();
        } catch (Exception ignored) {}
        return -1;
    }

    private String attr(XMLStreamReader r, String name) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            if (name.equals(r.getAttributeLocalName(i)))
                return r.getAttributeValue(i);
        }
        return null;
    }

    private BigDecimal parseAmount(String text) {
        return new BigDecimal(text.replace(",", "."));
    }
}