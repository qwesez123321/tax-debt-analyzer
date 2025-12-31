package ru.intelinfo.inczilla.mvc.repository;

import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CompanyDebtRepository {
    private final DatabaseManager db;

    public CompanyDebtRepository(DatabaseManager db) {
        this.db = db;
    }

    public void replaceAll(Map<String, CompanyDebtInfo> companies) throws SQLException {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate("delete from company_debt");
                    st.executeUpdate("delete from company");
                }

                try (PreparedStatement insertCompany =
                             conn.prepareStatement("insert into company(inn) values (?)");
                     PreparedStatement insertDebt =
                             conn.prepareStatement("insert into company_debt(inn, tax, amount) values (?, ?, ?)")) {

                    for (CompanyDebtInfo info : companies.values()) {
                        insertCompany.setString(1, info.inn);
                        insertCompany.addBatch();

                        for (Map.Entry<String, BigDecimal> e : info.debtByTaxType.entrySet()) {
                            insertDebt.setString(1, info.inn);
                            insertDebt.setString(2, e.getKey());
                            insertDebt.setBigDecimal(3, e.getValue());
                            insertDebt.addBatch();
                        }
                    }

                    insertCompany.executeBatch();
                    insertDebt.executeBatch();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Map<String, CompanyDebtInfo> loadAll() throws SQLException {
        Map<String, CompanyDebtInfo> result = new HashMap<>();

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("select inn from company");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String inn = rs.getString(1);
                    result.put(inn, new CompanyDebtInfo(inn));
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("select inn, tax, amount from company_debt");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String inn = rs.getString(1);
                    String tax = rs.getString(2);
                    BigDecimal amount = rs.getBigDecimal(3);

                    CompanyDebtInfo info = result.get(inn);
                    if (info != null) {
                        info.addDebt(tax, amount);
                    }
                }
            }
        }

        return result;
    }
}