package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

class DateHackTest {
    @Test
    void makeDbDateOld() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:./data/taxdebt;AUTO_SERVER=TRUE", "sa", "");
             Statement s = c.createStatement()) {
            s.executeUpdate("update dataset_meta set dataset_date = DATE '2000-01-01' where id = 1");
        }
    }
}