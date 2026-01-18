package ru.intelinfo.inczilla.mvc.repository;

import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CompanyDebtRepository {

    public void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                create table if not exists company (
                  inn varchar(12) primary key
                )
            """);

            st.executeUpdate("""
                create table if not exists company_debt (
                  inn varchar(12) not null,
                  tax varchar(1000) not null,
                  amount decimal(19,2) not null,
                  primary key (inn, tax),
                  foreign key (inn) references company(inn)
                )
            """);
        }
    }

    public void deleteAll(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("delete from company_debt");
            st.executeUpdate("delete from company");
        }
    }


    public void upsertCompany(Connection conn, PreparedStatement mergeCompany, String inn) throws SQLException {
        mergeCompany.setString(1, inn);
        mergeCompany.executeUpdate();
    }

    public void insertCompaniesBatch(Connection conn, PreparedStatement insertCompany, String inn) throws SQLException {
        insertCompany.setString(1, inn);
        insertCompany.addBatch();
    }

    public void insertDebtBatch(Connection conn, PreparedStatement insertDebt, String inn, String tax, BigDecimal amount) throws SQLException {
        insertDebt.setString(1, inn);
        insertDebt.setString(2, tax);
        insertDebt.setBigDecimal(3, amount);
        insertDebt.addBatch();
    }



    public void insertCompaniesFromStage(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                insert into company(inn)
                select inn from company_stage
            """);
        }
    }

    public void insertDebtsFromStage(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                insert into company_debt(inn, tax, amount)
                select inn, tax, amount from company_debt_stage
            """);
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
                BigDecimal amount = rs.getBigDecimal(3);

                CompanyDebtInfo info = result.get(inn);
                if (info != null) info.addDebt(tax, amount);
            }
        }

        return result;
    }
}