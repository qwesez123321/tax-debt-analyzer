package ru.intelinfo.inczilla.mvc.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class TaxdebtAnalyzerStartTest {

    private final TaxDebtAnalyzer analyzer = new TaxDebtAnalyzer();

    @Test
    @EnabledIfSystemProperty(named = "it", matches = "true")
    void runProgram_integrationTest() {
        analyzer.run("https://www.nalog.gov.ru/opendata/7707329152-debtam/");
    }
}