package ru.intelinfo.inczilla.mvc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final String jdbcUrl;
    private final String user;
    private final String password;

    public DatabaseManager(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    public void initSchema() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            st.executeUpdate("""
                create table if not exists dataset_meta (
                  id int primary key,
                  dataset_date date not null
                )
            """);

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
}