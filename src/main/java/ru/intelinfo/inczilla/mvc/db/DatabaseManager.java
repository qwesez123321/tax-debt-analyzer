package ru.intelinfo.inczilla.mvc.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Function;

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

    public <T> T withConnection(Function<Connection, T> action) {
        try (Connection conn = getConnection()) {
            return action.apply(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
    }


    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T t) throws SQLException;
    }

    public <T> T withConnectionSql(SqlFunction<Connection, T> action) {
        try (Connection conn = getConnection()) {
            return action.apply(conn);
        } catch (SQLException e) {
            throw new RuntimeException("DB error", e);
        }
    }
}