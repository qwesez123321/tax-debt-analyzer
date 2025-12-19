package ru.intelinfo.inczilla.mvc.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CompanyDebtInfo {
    public final String inn;
    public BigDecimal totalDebt = BigDecimal.ZERO;
    public final Map<String, BigDecimal> debtByTaxType = new HashMap<>();

    public CompanyDebtInfo(String inn) {
        this.inn = inn;
    }

    public void addDebt(String tax, BigDecimal amount) {
        totalDebt = totalDebt.add(amount);
        debtByTaxType.merge(tax, amount, BigDecimal::add);
    }
}