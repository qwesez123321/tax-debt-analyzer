package ru.intelinfo.inczilla.mvc.service;

import org.junit.jupiter.api.Test;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.io.ArchiveDownloader;
import ru.intelinfo.inczilla.mvc.io.OpenDataArchivePageClient;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.report.DebtStatisticsPrinter;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

class TaxdebtAnalyzerStart {

    @Test
    void runProgram_integrationTest() {
        String jdbcUrl = "jdbc:h2:file:./data/taxdebt;AUTO_SERVER=TRUE"; // чтобы было сравнение дат между запусками

        DatabaseManager db = new DatabaseManager(jdbcUrl, "sa", "");
        DebtStorageService storage = new DebtStorageService(
                db,
                new DatasetMetaRepository(),
                new CompanyDebtRepository()
        );

        TaxDebtAnalyzer analyzer = new TaxDebtAnalyzer(
                new OpenDataArchivePageClient(),
                new ArchiveDownloader(),
                new ZipXmlDebtParser(),
                new DebtStatisticsCalculator(),
                new DebtStatisticsPrinter(),
                storage
        );

        analyzer.run("https://www.nalog.gov.ru/opendata/7707329152-debtam/");
    }
}
