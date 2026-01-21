package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
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

    public Map<String, CompanyDebtInfo> loadAll() {
        return db.withConnectionSql(conn -> debtRepo.loadAll(conn));
    }

    public void replaceAllAtomically(Map<String, CompanyDebtInfo> companies, LocalDate datasetDate) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                debtRepo.clearStage(conn);
                debtRepo.saveToStage(conn, companies);
                debtRepo.replaceMainWithStage(conn);
                metaRepo.saveDatasetDate(conn, datasetDate);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Ошибка атомарного обновления БД", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка доступа к БД", e);
        }
    }

    private LocalDate getDbDatasetDate() {
        return db.withConnectionSql(conn -> metaRepo.getDatasetDate(conn));
    }
}