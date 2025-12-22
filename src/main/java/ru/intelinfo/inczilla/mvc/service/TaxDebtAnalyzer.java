package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TaxDebtAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(TaxDebtAnalyzer.class);

    private final OpenDataArchivePageClient pageClient = new OpenDataArchivePageClient();
    private final ArchiveDownloader downloader = new ArchiveDownloader();
    private final ZipXmlDebtParser parser = new ZipXmlDebtParser();
    private final DebtStatisticsCalculator calculator = new DebtStatisticsCalculator();
    private final DebtStatisticsPrinter printer = new DebtStatisticsPrinter();

    public void run(String dataUrl) {
        Path zipPath = null;

        try {
            String archiveUrl = pageClient.getArchiveUrl(dataUrl);

            if (archiveUrl == null) {
                log.error("Не удалось найти ссылку на архив на странице {}", dataUrl);
                return;
            }

            log.info("Найден архив: {}", archiveUrl);

            zipPath = downloader.downloadArchiveToTemp(archiveUrl);
            if (zipPath == null) {
                log.error("Ошибка при скачивании архива");
                return;
            }

            Map<String, CompanyDebtInfo> companies = parser.parseArchive(zipPath.toString());

            var stats = calculator.calculate(companies);
            printer.print(stats);

        } catch (Exception e) {
            log.error("Ошибка выполнения программы", e);

        } finally {
            if (zipPath != null) {
                try {
                    Files.deleteIfExists(zipPath);
                    log.info("Временный архив удалён: {}", zipPath);
                } catch (IOException ex) {
                    log.warn("Не удалось удалить временный архив: {}", zipPath, ex);
                }
            }
        }
    }
}