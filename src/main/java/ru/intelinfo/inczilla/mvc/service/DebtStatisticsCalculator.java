package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.model.DebtStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class DebtStatisticsCalculator {

    private final DebtStorageService storage;

    public DebtStatisticsCalculator(DebtStorageService storage) {
        this.storage = storage;
    }

    public DebtStatistics calculate() {
        int count = storage.countCompanies();
        if (count == 0) {
            return new DebtStatistics(0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
        }

        long maxTotalKopeks = storage.maxTotalDebtPerCompanyKopeks();
        long avgTotalKopeks = storage.avgTotalDebtPerCompanyKopeks();
        Map<String, Long> maxByTaxKopeks = storage.maxDebtByTaxTypeKopeks();

        BigDecimal maxTotal = BigDecimal.valueOf(maxTotalKopeks).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        BigDecimal avg = BigDecimal.valueOf(avgTotalKopeks).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);

        Map<String, BigDecimal> maxByTax = new HashMap<>();
        for (var e : maxByTaxKopeks.entrySet()) {
            maxByTax.put(e.getKey(),
                    BigDecimal.valueOf(e.getValue()).movePointLeft(2).setScale(2, RoundingMode.HALF_UP));
        }

        return new DebtStatistics(count, maxTotal, avg, maxByTax);
    }
}