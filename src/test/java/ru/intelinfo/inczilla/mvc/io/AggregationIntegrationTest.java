package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;
import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AggregationIntegrationTest {

    @Test
    void shouldAggregateStageDataCorrectly() throws Exception {

        DatabaseManager db = new DatabaseManager(
                "jdbc:h2:mem:agg_test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                "sa",
                ""
        );

        CompanyDebtRepository repo = new CompanyDebtRepository();
        DatasetMetaRepository meta = new DatasetMetaRepository();

        try (Connection conn = db.getConnection()) {
            repo.initSchema(conn);
            repo.initStageSchema(conn);
            meta.initSchema(conn);

            short taxId = repo.getOrCreateTaxTypeId(conn, "Тестовый налог");

            try (PreparedStatement ps = conn.prepareStatement(
                    "insert into debt_stage_raw(inn, tax_type_id, amount_kopeks) values (?,?,?)")) {

                ps.setLong(1, 1234567890L);
                ps.setShort(2, taxId);
                ps.setLong(3, 1000);
                ps.addBatch();

                ps.setLong(1, 1234567890L);
                ps.setShort(2, taxId);
                ps.setLong(3, 2000);
                ps.addBatch();

                ps.executeBatch();
            }

            repo.replaceMainWithStageAggregated(conn);
            repo.rebuildCompanyTotals(conn);

            long max = repo.maxTotalDebtPerCompanyKopeks(conn);
            int count = repo.countCompanies(conn);

            assertEquals(3000, max);
            assertEquals(1, count);
        }
    }
}