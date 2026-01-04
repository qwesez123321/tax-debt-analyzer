package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;
import ru.intelinfo.inczilla.mvc.util.ArchiveDatasetDateExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

public class TaxDebtAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(TaxDebtAnalyzer.class);

    private final OpenDataArchivePageClient pageClient;
    private final ArchiveDownloader downloader;
    private final ZipXmlDebtParser parser;
    private final DebtStatisticsCalculator calculator;
    private final DebtStatisticsPrinter printer;
    private final DebtStorageService storage;

    public TaxDebtAnalyzer(OpenDataArchivePageClient pageClient,
                           ArchiveDownloader downloader,
                           ZipXmlDebtParser parser,
                           DebtStatisticsCalculator calculator,
                           DebtStatisticsPrinter printer,
                           DebtStorageService storage) {
        this.pageClient = pageClient;
        this.downloader = downloader;
        this.parser = parser;
        this.calculator = calculator;
        this.printer = printer;
        this.storage = storage;
    }

    public void run(String dataUrl) {
        Path zipPath = null;

        try {
            storage.ensureSchema();

            String archiveUrl = pageClient.getArchiveUrl(dataUrl);
            if (archiveUrl == null) {
                log.error("Не удалось найти ссылку на архив на странице {}", dataUrl);
                return;
            }

            LocalDate siteDate = ArchiveDatasetDateExtractor.extractFromArchiveUrl(archiveUrl);
            if (siteDate == null) {
                log.error("Не удалось определить дату набора данных из ссылки: {}", archiveUrl);
                return;
            }

            LocalDate dbDate = storage.getDbDatasetDate();
            log.info("Дата набора на сайте: {}", siteDate);
            log.info("Дата набора в БД: {}", dbDate);

            Map<String, CompanyDebtInfo> companies;

            boolean needUpdate = (dbDate == null) || dbDate.isBefore(siteDate);

            if (needUpdate) {
                log.info("Данные в БД отсутствуют или устарели → обновляем");

                zipPath = downloader.downloadArchiveToTemp(archiveUrl);
                if (zipPath == null) {
                    log.error("Ошибка при скачивании архива");
                    return;
                }

                companies = parser.parseArchive(zipPath.toString());
                storage.replaceAll(companies, siteDate);

            } else {
                log.info("Данные в БД актуальны → читаем из БД");
                companies = storage.loadAll();
            }

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
