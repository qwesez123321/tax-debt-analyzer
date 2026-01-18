package ru.intelinfo.inczilla.mvc.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

class TaxdebtAnalyzerStart {

    /**
     * Ручной запуск из IDE.
     * Отключён по умолчанию, потому что:
     *  - зависит от сети
     *  - скачивает большой архив
     *  - может падать при недоступности ФНС (это нормально для ручного запуска)
     */
    @Disabled("Manual run (uses network + downloads big dataset)")
    @Test
    void runProgram_manual() {
        // данные сохраняются между запусками
        String jdbcUrl = "jdbc:h2:file:./data/taxdebt;AUTO_SERVER=TRUE";



        DatabaseManager db = new DatabaseManager(jdbcUrl, "sa", "");

        DatasetMetaRepository metaRepo = new DatasetMetaRepository();
        CompanyDebtRepository debtRepo = new CompanyDebtRepository();

        DebtStorageService storage = new DebtStorageService(db, metaRepo, debtRepo);
        DatasetUpdateService updateService = new DatasetUpdateService(storage);
        DebtStatisticsCalculator calculator = new DebtStatisticsCalculator(storage);

        TaxDebtAnalyzer analyzer = new TaxDebtAnalyzer(
                new OpenDataArchivePageClient(),
                new ArchiveDownloader(),
                new ZipXmlDebtParser(),
                updateService,
                calculator,
                new DebtStatisticsPrinter()
        );

        analyzer.run("https://www.nalog.gov.ru/opendata/7707329152-debtam/");
    }
}