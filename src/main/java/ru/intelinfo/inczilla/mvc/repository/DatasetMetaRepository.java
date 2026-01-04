package ru.intelinfo.inczilla.mvc.repository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class DatasetMetaRepository {

    public void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                create table if not exists dataset_meta (
                  id int primary key,
                  dataset_date date not null
                )
            """);
        }
    }

    public LocalDate getDatasetDate(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "select dataset_date from dataset_meta where id = 1"
        )) {
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date d = rs.getDate(1);
                return d != null ? d.toLocalDate() : null;
            }
        }
    }

    public void saveDatasetDate(Connection conn, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "merge into dataset_meta (id, dataset_date) key(id) values (1, ?)"
        )) {
            ps.setDate(1, Date.valueOf(date));
            ps.executeUpdate();
        }
    }
}
