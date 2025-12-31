package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;
import ru.intelinfo.inczilla.mvc.util.ArchiveDatasetDateExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

public class TaxDebtAnalyzer {
    private static final Logger log = LoggerFactory.getLogger(TaxDebtAnalyzer.class);

    private final OpenDataArchivePageClient pageClient = new OpenDataArchivePageClient();
    private final ArchiveDownloader downloader = new ArchiveDownloader();
    private final ZipXmlDebtParser parser = new ZipXmlDebtParser();
    private final DebtStatisticsCalculator calculator = new DebtStatisticsCalculator();
    private final DebtStatisticsPrinter printer = new DebtStatisticsPrinter();

    private final DatabaseManager db;
    private final DatasetMetaRepository metaRepo;
    private final CompanyDebtRepository debtRepo;

    public TaxDebtAnalyzer() {
        String mode = System.getProperty("db.mode", "file"); // file | mem

        String jdbcUrl;
        if ("mem".equalsIgnoreCase(mode)) {
            jdbcUrl = "jdbc:h2:mem:taxdebt;DB_CLOSE_DELAY=-1";
        } else {
            jdbcUrl = "jdbc:h2:file:./data/taxdebt;AUTO_SERVER=TRUE";
        }

        this.db = new DatabaseManager(jdbcUrl, "sa", "");
        this.metaRepo = new DatasetMetaRepository(db);
        this.debtRepo = new CompanyDebtRepository(db);
    }

    public void run(String dataUrl) {
        Path zipPath = null;

        try {
            db.initSchema();

            String archiveUrl = pageClient.getArchiveUrl(dataUrl);
            if (archiveUrl == null) {
                log.error("Не удалось найти ссылку на архив на странице {}", dataUrl);
                return;
            }

            log.info("Найден архив: {}", archiveUrl);

            LocalDate siteDate = ArchiveDatasetDateExtractor.extractFromArchiveUrl(archiveUrl);
            if (siteDate == null) {
                log.error("Не удалось определить дату набора данных из ссылки: {}", archiveUrl);
                return;
            }
            log.info("Дата набора данных на сайте: {}", siteDate);

            LocalDate dbDate = metaRepo.getDatasetDate();
            log.info("Дата набора данных в БД: {}", dbDate);

            Map<String, CompanyDebtInfo> companies;

            boolean needUpdate = (dbDate == null) || dbDate.isBefore(siteDate);

            if (needUpdate) {
                log.info("Данные в БД отсутствуют или устарели → обновляем из архива");

                zipPath = downloader.downloadArchiveToTemp(archiveUrl);
                if (zipPath == null) {
                    log.error("Ошибка при скачивании архива");
                    return;
                }

                companies = parser.parseArchive(zipPath.toString());

                debtRepo.replaceAll(companies);
                metaRepo.saveDatasetDate(siteDate);

                log.info("БД обновлена. Сохранена дата набора: {}", siteDate);

            } else {
                log.info("Данные в БД актуальны → скачивание не требуется, читаем из БД");
                companies = debtRepo.loadAll();
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