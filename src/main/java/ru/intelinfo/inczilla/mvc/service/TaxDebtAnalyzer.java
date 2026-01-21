package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;
import ru.intelinfo.inczilla.mvc.util.ArchiveDatasetDateExtractor;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

public class TaxDebtAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(TaxDebtAnalyzer.class);

    private final OpenDataArchivePageClient pageClient;
    private final ArchiveDownloader downloader;
    private final ZipXmlDebtParser parser;

    private final DatasetUpdateService updateService;
    private final DebtStatisticsCalculator calculator;
    private final DebtStatisticsPrinter printer;

    public TaxDebtAnalyzer(OpenDataArchivePageClient pageClient,
                           ArchiveDownloader downloader,
                           ZipXmlDebtParser parser,
                           DatasetUpdateService updateService,
                           DebtStatisticsCalculator calculator,
                           DebtStatisticsPrinter printer) {
        this.pageClient = pageClient;
        this.downloader = downloader;
        this.parser = parser;
        this.updateService = updateService;
        this.calculator = calculator;
        this.printer = printer;
    }

    public void run(String dataUrl) {
        try {
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

            updateService.updateIfRequired(siteDate, () -> {
                Path zipPath = downloader.downloadArchiveToTemp(archiveUrl);
                if (zipPath == null) throw new IllegalStateException("Ошибка скачивания архива");
                Map<String, CompanyDebtInfo> companies = parser.parseArchive(zipPath.toString());
                return new ParsedArchive(zipPath, companies);
            });

            var stats = calculator.calculate();
            printer.print(stats);

        } catch (Exception e) {
            log.error("Ошибка выполнения", e);
        }
    }

    public static final class ParsedArchive {
        public final Path zipPath;
        public final Map<String, CompanyDebtInfo> companies;

        public ParsedArchive(Path zipPath, Map<String, CompanyDebtInfo> companies) {
            this.zipPath = zipPath;
            this.companies = companies;
        }
    }
}