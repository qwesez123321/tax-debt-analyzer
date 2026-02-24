package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;
import ru.intelinfo.inczilla.mvc.service.DebtStorageService;

import java.sql.Connection;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatasetUpdateLogicTest {

    @Test
    void shouldNotUpdateIfDatesAreEqual() throws Exception {

        DatabaseManager db = new DatabaseManager(
                "jdbc:h2:mem:date_test_1;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa",
                ""
        );

        DatasetMetaRepository meta = new DatasetMetaRepository();
        CompanyDebtRepository repo = new CompanyDebtRepository();

        try (Connection conn = db.getConnection()) {
            repo.initSchema(conn);
            meta.initSchema(conn);

            LocalDate today = LocalDate.now();
            meta.saveDatasetDate(conn, today);
        }

        DebtStorageService service = new DebtStorageService(db, meta, repo);

        assertFalse(service.isUpdateRequired(LocalDate.now()));
    }

    @Test
    void shouldUpdateIfSiteDateIsNewer() throws Exception {

        DatabaseManager db = new DatabaseManager(
                "jdbc:h2:mem:date_test_2;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa",
                ""
        );

        DatasetMetaRepository meta = new DatasetMetaRepository();
        CompanyDebtRepository repo = new CompanyDebtRepository();

        try (Connection conn = db.getConnection()) {
            repo.initSchema(conn);
            meta.initSchema(conn);

            meta.saveDatasetDate(conn, LocalDate.of(2020, 1, 1));
        }

        DebtStorageService service = new DebtStorageService(db, meta, repo);

        assertTrue(service.isUpdateRequired(LocalDate.of(2020, 1, 2)));
    }
}