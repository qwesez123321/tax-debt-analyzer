package ru.intelinfo.inczilla.mvc.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipXmlDebtParser {
    private static final Logger log = LoggerFactory.getLogger(ZipXmlDebtParser.class);
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

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


    @FunctionalInterface
    public interface DebtRowConsumer {
        void accept(String inn, String taxName, BigDecimal amount);
    }


    public void parseArchiveStreaming(String zipFilePath, DebtRowConsumer consumer)
            throws IOException, XMLStreamException {

        log.info("Открываем архив (streaming): {}", zipFilePath);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    log.info("Чтение XML: {}", entry.getName());

                    parseSingleXmlStreaming(zis, consumer);
                }

                zis.closeEntry();
            }
        }
    }


    private static class PendingDebt {
        final String tax;
        final BigDecimal amount;

        PendingDebt(String tax, BigDecimal amount) {
            this.tax = tax;
            this.amount = amount;
        }
    }

    private static final class NonClosingInputStream extends FilterInputStream {
        private NonClosingInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {

        }
    }


    private void parseSingleXml(InputStream xmlStream, Map<String, CompanyDebtInfo> map) throws XMLStreamException {
        XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(xmlStream);

        boolean insideDocument = false;
        String currentInn = null;
        List<PendingDebt> pendingDebts = new ArrayList<>();

        while (r.hasNext()) {
            int eventType = r.next();

            if (eventType == XMLStreamConstants.START_ELEMENT) {
                String name = r.getLocalName();

                if ("Документ".equals(name)) {
                    insideDocument = true;
                    currentInn = null;
                    pendingDebts.clear();
                    continue;
                }

                if (!insideDocument) continue;

                if ("СведНедоим".equals(name)) {
                    String tax = attr(r, "НаимНалог");
                    String sum = attr(r, "ОбщСумНедоим");

                    if (tax == null || sum == null) continue;

                    BigDecimal amount;
                    try {
                        amount = parseAmount(sum);
                    } catch (NumberFormatException ex) {
                        continue;
                    }

                    if (currentInn == null) {
                        pendingDebts.add(new PendingDebt(tax, amount));
                    } else {
                        map.get(currentInn).addDebt(tax, amount);
                    }
                    continue;
                }

                if ("СведНП".equals(name)) {
                    String inn = attr(r, "ИННЮЛ");
                    if (inn == null || inn.isBlank()) {
                        currentInn = null;
                        pendingDebts.clear();
                        continue;
                    }

                    currentInn = inn;
                    map.putIfAbsent(inn, new CompanyDebtInfo(inn));

                    if (!pendingDebts.isEmpty()) {
                        CompanyDebtInfo info = map.get(inn);
                        for (PendingDebt d : pendingDebts) {
                            info.addDebt(d.tax, d.amount);
                        }
                        pendingDebts.clear();
                    }
                }

            } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                if ("Документ".equals(r.getLocalName())) {
                    insideDocument = false;
                    currentInn = null;
                    pendingDebts.clear();
                }
            }
        }
    }

    private void parseSingleXmlStreaming(InputStream xmlStream, DebtRowConsumer consumer) throws XMLStreamException {

        InputStream safeStream = new NonClosingInputStream(xmlStream);

        XMLStreamReader r = XML_INPUT_FACTORY.createXMLStreamReader(safeStream);

        boolean insideDocument = false;
        String currentInn = null;
        List<PendingDebt> pendingDebts = new ArrayList<>();

        try {
            while (r.hasNext()) {
                int eventType = r.next();

                if (eventType == XMLStreamConstants.START_ELEMENT) {
                    String name = r.getLocalName();

                    if ("Документ".equals(name)) {
                        insideDocument = true;
                        currentInn = null;
                        pendingDebts.clear();
                        continue;
                    }

                    if (!insideDocument) continue;

                    if ("СведНедоим".equals(name)) {
                        String tax = attr(r, "НаимНалог");
                        String sum = attr(r, "ОбщСумНедоим");

                        if (tax == null || sum == null) continue;

                        BigDecimal amount;
                        try {
                            amount = parseAmount(sum);
                        } catch (NumberFormatException ex) {
                            continue;
                        }

                        if (currentInn == null) {
                            pendingDebts.add(new PendingDebt(tax, amount));
                        } else {
                            consumer.accept(currentInn, tax, amount);
                        }
                        continue;
                    }

                    if ("СведНП".equals(name)) {
                        String inn = attr(r, "ИННЮЛ");
                        if (inn == null || inn.isBlank()) {
                            currentInn = null;
                            pendingDebts.clear();
                            continue;
                        }

                        currentInn = inn;

                        if (!pendingDebts.isEmpty()) {
                            for (PendingDebt d : pendingDebts) {
                                consumer.accept(currentInn, d.tax, d.amount);
                            }
                            pendingDebts.clear();
                        }
                    }

                } else if (eventType == XMLStreamConstants.END_ELEMENT) {
                    if ("Документ".equals(r.getLocalName())) {
                        insideDocument = false;
                        currentInn = null;
                        pendingDebts.clear();
                    }
                }
            }
        } finally {
            try {
                r.close();
            } catch (Exception ignore) {
            }
        }
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        return out.toByteArray();
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