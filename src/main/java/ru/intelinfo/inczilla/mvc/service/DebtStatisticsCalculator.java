package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.model.CompanyDebtInfo;
import ru.intelinfo.inczilla.mvc.model.DebtStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class DebtStatisticsCalculator {

    public DebtStatistics calculate(Map<String, CompanyDebtInfo> map) {
        int count = map.size();
        if (count == 0) {
            return new DebtStatistics(0, BigDecimal.ZERO, BigDecimal.ZERO, Map.of());
        }

        BigDecimal maxTotal = BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        Map<String, BigDecimal> maxByTax = new HashMap<>();

        for (CompanyDebtInfo info : map.values()) {
            sum = sum.add(info.totalDebt);

            if (info.totalDebt.compareTo(maxTotal) > 0) {
                maxTotal = info.totalDebt;
            }

            for (var e : info.debtByTaxType.entrySet()) {
                maxByTax.merge(e.getKey(), e.getValue(), BigDecimal::max);
            }
        }

        BigDecimal avg = sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        return new DebtStatistics(count, maxTotal, avg, maxByTax);
    }
}