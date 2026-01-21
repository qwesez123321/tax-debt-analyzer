package ru.intelinfo.inczilla.mvc.repository;

import java.sql.*;
import java.time.LocalDate;

public class DatasetMetaRepository {

    private static final String DATASET_ID = "7707329152-debtam";

    public void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("""
                create table if not exists dataset_meta (
                  dataset_id varchar(64) primary key,
                  dataset_date date
                )
            """);
        }
    }

    public LocalDate getDatasetDate(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "select dataset_date from dataset_meta where dataset_id = ?")) {
            ps.setString(1, DATASET_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date d = rs.getDate(1);
                return d == null ? null : d.toLocalDate();
            }
        }
    }

    public void saveDatasetDate(Connection conn, LocalDate date) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
            merge into dataset_meta(dataset_id, dataset_date)
            key(dataset_id)
            values (?, ?)
        """)) {
            ps.setString(1, DATASET_ID);
            ps.setDate(2, date == null ? null : Date.valueOf(date));
            ps.executeUpdate();
        }
    }
}
