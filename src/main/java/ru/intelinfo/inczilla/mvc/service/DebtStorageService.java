package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.model.DebtStatistics;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class DebtStorageService {
    private static final Logger log = LoggerFactory.getLogger(DebtStorageService.class);

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

    public void ensureSchema() throws SQLException {
        db.withConnection(conn -> {
            try {
                metaRepo.initSchema(conn);
                debtRepo.initSchema(conn);

                // staging-таблицы для атомарного обновления
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS company_stage (
                          inn VARCHAR(12) PRIMARY KEY
                        )
                        """);

                    st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS company_debt_stage (
                          inn VARCHAR(12) NOT NULL,
                          tax VARCHAR(1024) NOT NULL,
                          amount DECIMAL(30,2) NOT NULL,
                          PRIMARY KEY (inn, tax)
                        )
                        """);
                }

                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public LocalDate getDbDatasetDate() throws SQLException {
        return db.withConnection(conn -> {
            try {
                return metaRepo.getDatasetDate(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public boolean isUpdateRequired(LocalDate siteDate) throws SQLException {
        LocalDate dbDate = getDbDatasetDate();
        log.info("Дата набора на сайте: {}", siteDate);
        log.info("Дата набора в БД: {}", dbDate);
        return dbDate == null || dbDate.isBefore(siteDate);
    }

    public Map<String, CompanyDebtInfo> loadAll() throws SQLException {
        return db.withConnection(conn -> {
            try {
                return debtRepo.loadAll(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void replaceAllAtomically(Map<String, CompanyDebtInfo> companies, LocalDate datasetDate) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {

                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("DELETE FROM company_debt_stage");
                    st.executeUpdate("DELETE FROM company_stage");
                }

                try (PreparedStatement mergeCompanyStage =
                             conn.prepareStatement("MERGE INTO company_stage(inn) KEY(inn) VALUES (?)");
                     PreparedStatement mergeDebtStage =
                             conn.prepareStatement("MERGE INTO company_debt_stage(inn, tax, amount) KEY(inn, tax) VALUES (?, ?, ?)")) {

                    for (CompanyDebtInfo info : companies.values()) {
                        mergeCompanyStage.setString(1, info.inn);
                        mergeCompanyStage.executeUpdate();

                        for (var e : info.debtByTaxType.entrySet()) {
                            mergeDebtStage.setString(1, info.inn);
                            mergeDebtStage.setString(2, e.getKey());
                            mergeDebtStage.setBigDecimal(3, e.getValue());
                            mergeDebtStage.addBatch();
                        }
                    }
                    mergeDebtStage.executeBatch();
                }


                debtRepo.deleteAll(conn);

                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("INSERT INTO company(inn) SELECT inn FROM company_stage");
                    st.executeUpdate("INSERT INTO company_debt(inn, tax, amount) SELECT inn, tax, amount FROM company_debt_stage");
                }

                metaRepo.saveDatasetDate(conn, datasetDate);

                conn.commit();
                log.info("Данные успешно сохранены в БД (атомарно). Дата набора: {}", datasetDate);
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    public DebtStatistics calculateStatistics() throws SQLException {
        Map<String, CompanyDebtInfo> map = loadAll();

        int count = map.size();
        if (count == 0) {
            return new DebtStatistics(0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
        }

        BigDecimal maxTotal = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        Map<String, BigDecimal> maxByTax = new HashMap<>();

        for (CompanyDebtInfo info : map.values()) {
            sum = sum.add(info.totalDebt);

            if (info.totalDebt.compareTo(maxTotal) > 0) {
                maxTotal = info.totalDebt;
            }

            for (var e : info.debtByTaxType.entrySet()) {
                maxByTax.merge(e.getKey(), e.getValue(), BigDecimal::max);
            }
        }

        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return new DebtStatistics(count, maxTotal, avg, maxByTax);
    }
}