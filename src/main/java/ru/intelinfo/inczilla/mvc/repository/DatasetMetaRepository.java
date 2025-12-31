package ru.intelinfo.inczilla.mvc.repository;

import ru.intelinfo.inczilla.mvc.db.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class DatasetMetaRepository {
    private final DatabaseManager db;

    public DatasetMetaRepository(DatabaseManager db) {
        this.db = db;
    }

    public LocalDate getDatasetDate() throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "select dataset_date from dataset_meta where id = 1"
             )) {

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date d = rs.getDate(1);
                return d != null ? d.toLocalDate() : null;
            }
        }
    }

    public void saveDatasetDate(LocalDate date) throws SQLException {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "merge into dataset_meta (id, dataset_date) key(id) values (1, ?)"
             )) {
            ps.setDate(1, Date.valueOf(date));
            ps.executeUpdate();
        }
    }
}