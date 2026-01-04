package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

public class DebtStorageService {
    private static final Logger log = LoggerFactory.getLogger(DebtStorageService.class);

    private final DatabaseManager db;
    private final DatasetMetaRepository metaRepo;
    private final CompanyDebtRepository debtRepo;

    // настройки пачек (чтобы не держать БД долго в блокировке)
    private final int companyBatchSize = 1000;
    private final int debtBatchSize = 5000;

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

    public Map<String, CompanyDebtInfo> loadAll() throws SQLException {
        return db.withConnection(conn -> {
            try {
                return debtRepo.loadAll(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Обновление данных:
     * 1) быстро очищаем таблицы (короткая транзакция)
     * 2) вставляем пачками и коммитим порциями
     * 3) сохраняем дату набора
     */
    public void replaceAll(Map<String, CompanyDebtInfo> companies, LocalDate datasetDate) throws SQLException {
        try (Connection conn = db.getConnection()) {

            // 1) быстрая очистка
            conn.setAutoCommit(false);
            try {
                debtRepo.deleteAll(conn);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }

            // 2) массовая вставка кусками
            conn.setAutoCommit(false);

            try (PreparedStatement mergeCompany =
                         conn.prepareStatement("merge into company(inn) key(inn) values (?)");
                 PreparedStatement insertDebt =
                         conn.prepareStatement("merge into company_debt(inn, tax, amount) key(inn, tax) values (?, ?, ?)")) {

                int opCounter = 0;
                final int commitEvery = 5000; // сколько операций перед коммитом

                for (CompanyDebtInfo info : companies.values()) {

                    // гарантируем, что компания существует в БД до долгов
                    mergeCompany.setString(1, info.inn);
                    mergeCompany.executeUpdate();
                    opCounter++;

                    for (var e : info.debtByTaxType.entrySet()) {
                        insertDebt.setString(1, info.inn);
                        insertDebt.setString(2, e.getKey());
                        insertDebt.setBigDecimal(3, e.getValue());
                        insertDebt.addBatch();
                        opCounter++;

                        if (opCounter >= commitEvery) {
                            insertDebt.executeBatch();
                            conn.commit();     // порционный коммит
                            opCounter = 0;
                        }
                    }
                }

                insertDebt.executeBatch();

                // 3) сохраняем дату набора
                metaRepo.saveDatasetDate(conn, datasetDate);

                conn.commit();
                log.info("Данные успешно сохранены в БД. Дата набора: {}", datasetDate);

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

}
