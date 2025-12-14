package ru.intelinfo.inczilla.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebtStatisticsPrinter {
    private static final Logger log = LoggerFactory.getLogger(DebtStatisticsPrinter.class);

    public void print(DebtStatistics s) {
        log.info("==============================================");
        log.info("ИТОГОВАЯ СТАТИСТИКА");
        log.info("==============================================");

        log.info("Всего компаний: {}", s.companiesCount);
        if (s.companiesCount == 0) return;

        log.info("Максимальный долг одной компании: {}", s.maxTotalDebt);

        log.info("Максимальные долги по типам:");
        for (var e : s.maxByTax.entrySet()) {
            log.info("  {} → {}", e.getKey(), e.getValue());
        }

        log.info("Средний долг: {}", s.avgDebt);
    }
}