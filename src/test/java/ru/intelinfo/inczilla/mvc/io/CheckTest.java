package ru.intelinfo.inczilla.mvc.io;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class CheckTest {
    @Test
    void checkTables() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:h2:file:./data/taxdebt;AUTO_SERVER=TRUE", "sa", "");
             Statement s = c.createStatement()) {

            try (ResultSet rs = s.executeQuery("select count(*) from company")) {
                rs.next();
                System.out.println("company count = " + rs.getInt(1));
            }

            try (ResultSet rs = s.executeQuery("select count(*) from company_debt")) {
                rs.next();
                System.out.println("company_debt count = " + rs.getInt(1));
            }

            try (ResultSet rs = s.executeQuery("select dataset_date from dataset_meta where id=1")) {
                rs.next();
                System.out.println("dataset_date = " + rs.getDate(1));
            }
        }
    }
}