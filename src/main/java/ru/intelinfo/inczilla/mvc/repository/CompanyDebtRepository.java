package ru.intelinfo.inczilla.mvc.repository;

import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CompanyDebtRepository {

    public void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                create table if not exists company (
                  inn varchar(10) primary key
                )
            """);

            st.executeUpdate("""
                create table if not exists company_debt (
                  inn varchar(10) not null,
                  tax varchar(1000) not null,
                  amount decimal(19,2) not null,
                  primary key (inn, tax),
                  foreign key (inn) references company(inn)
                )
            """);
        }
    }

    public void initStageSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                create table if not exists company_stage (
                  inn varchar(10) primary key
                )
            """);

            st.executeUpdate("""
                create table if not exists company_debt_stage (
                  inn varchar(10) not null,
                  tax varchar(1000) not null,
                  amount decimal(19,2) not null,
                  primary key (inn, tax)
                )
            """);
        }
    }

    public void clearStage(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("delete from company_debt_stage");
            st.executeUpdate("delete from company_stage");
        }
    }

    public void saveToStage(Connection conn, Map<String, CompanyDebtInfo> companies) throws SQLException {
        try (PreparedStatement mergeCompany =
                     conn.prepareStatement("merge into company_stage(inn) key(inn) values (?)");
             PreparedStatement mergeDebt =
                     conn.prepareStatement("merge into company_debt_stage(inn, tax, amount) key(inn, tax) values (?, ?, ?)")) {

            int batch = 0;
            final int batchSize = 5000;

            for (CompanyDebtInfo info : companies.values()) {
                mergeCompany.setString(1, info.inn);
                mergeCompany.addBatch();

                for (var e : info.debtByTaxType.entrySet()) {
                    mergeDebt.setString(1, info.inn);
                    mergeDebt.setString(2, e.getKey());
                    mergeDebt.setBigDecimal(3, e.getValue());
                    mergeDebt.addBatch();

                    if (++batch >= batchSize) {
                        mergeDebt.executeBatch();
                        batch = 0;
                    }
                }
            }

            mergeCompany.executeBatch();
            mergeDebt.executeBatch();
        }
    }

    public void replaceMainWithStage(Connection conn) throws SQLException {
        deleteAll(conn);

        try (Statement st = conn.createStatement()) {
            st.executeUpdate("insert into company(inn) select inn from company_stage");
            st.executeUpdate("insert into company_debt(inn, tax, amount) select inn, tax, amount from company_debt_stage");
        }
    }

    public void deleteAll(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("delete from company_debt");
            st.executeUpdate("delete from company");
        }
    }

    public Map<String, CompanyDebtInfo> loadAll(Connection conn) throws SQLException {
        Map<String, CompanyDebtInfo> result = new HashMap<>();

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
                var amount = rs.getBigDecimal(3);

                CompanyDebtInfo info = result.get(inn);
                if (info != null) info.addDebt(tax, amount);
    }
}

        return result;
    }
}