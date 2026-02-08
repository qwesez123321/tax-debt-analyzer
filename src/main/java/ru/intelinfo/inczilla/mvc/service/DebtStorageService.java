package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DebtStorageService {

    private final DatabaseManager db;
    private final DatasetMetaRepository metaRepo;
    private final CompanyDebtRepository debtRepo;

    public DebtStorageService(DatabaseManager db,
                              DatasetMetaRepository metaRepo,
                              CompanyDebtRepository debtRepo) {
        this.db = db;
        this.metaRepo = metaRepo;
        this.debtRepo = debtRepo;
    }

    public boolean isUpdateRequired(LocalDate siteDate) {
        LocalDate dbDate = getDbDatasetDate();
        return dbDate == null || dbDate.isBefore(siteDate);
    }

    public void replaceAllAtomicallyFromZip(String zipPath, LocalDate datasetDate, ZipXmlDebtParser parser) {
        try (Connection conn = db.getConnection()) {

            conn.setAutoCommit(false);

            debtRepo.clearStage(conn);

            final Map<String, Short> taxCache = new HashMap<>(128);

            try (PreparedStatement ins = conn.prepareStatement(
                    "insert into debt_stage_raw(inn, tax_type_id, amount_kopeks) values (?,?,?)")) {

                final int batchSize = 5000;
                final int[] batch = {0};

                parser.parseArchiveStreaming(zipPath, (innStr, taxName, amount) -> {
                    try {
                        long inn = Long.parseLong(innStr);

                        short taxId = taxCache.computeIfAbsent(taxName, t -> {
                            try {
                                return debtRepo.getOrCreateTaxTypeId(conn, t);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        long kopeks = amount
                                .movePointRight(2)
                                .setScale(0, RoundingMode.HALF_UP)
                                .longValueExact();

                        ins.setLong(1, inn);
                        ins.setShort(2, taxId);
                        ins.setLong(3, kopeks);
                        ins.addBatch();

                        if (++batch[0] >= batchSize) {
                            ins.executeBatch();
                            batch[0] = 0;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                if (batch[0] > 0) {
                    ins.executeBatch();
                }
            }

            debtRepo.replaceMainWithStageAggregated(conn);

            metaRepo.saveDatasetDate(conn, datasetDate);

            conn.commit();

        } catch (Exception e) {
            throw new RuntimeException("Ошибка атомарного обновления БД из ZIP", e);
        }
    }


    public int countCompanies() {
        return db.withConnectionSql(debtRepo::countCompanies);
    }

    public long maxTotalDebtPerCompanyKopeks() {
        return db.withConnectionSql(debtRepo::maxTotalDebtPerCompanyKopeks);
    }

    public long avgTotalDebtPerCompanyKopeks() {
        return db.withConnectionSql(debtRepo::avgTotalDebtPerCompanyKopeks);
    }

    public Map<String, Long> maxDebtByTaxTypeKopeks() {
        return db.withConnectionSql(debtRepo::maxDebtByTaxTypeKopeks);
    }

    private LocalDate getDbDatasetDate() {
        return db.withConnectionSql(metaRepo::getDatasetDate);
    }
}