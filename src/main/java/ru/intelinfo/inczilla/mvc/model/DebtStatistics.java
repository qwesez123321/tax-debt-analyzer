package ru.intelinfo.inczilla.mvc.model;

import java.math.BigDecimal;
import java.util.Map;

public class DebtStatistics {
    public final int companiesCount;
    public final BigDecimal maxTotalDebt;
    public final BigDecimal avgDebt;
    public final Map<String, BigDecimal> maxByTax;

    public DebtStatistics(int companiesCount,
                          BigDecimal maxTotalDebt,
                          BigDecimal avgDebt,
                          Map<String, BigDecimal> maxByTax) {
        this.companiesCount = companiesCount;
        this.maxTotalDebt = maxTotalDebt;
        this.avgDebt = avgDebt;
        this.maxByTax = maxByTax;
    }
}