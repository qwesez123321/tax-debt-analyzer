package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.model.DebtStatistics;

public class DebtStatisticsCalculator {

    private final DebtStorageService storage;

    public DebtStatisticsCalculator(DebtStorageService storage) {
        this.storage = storage;
    }

    public DebtStatistics calculate() {
        try {
            return storage.calculateStatistics();
        } catch (Exception e) {
            throw new RuntimeException("Не удалось посчитать статистику из БД", e);
        }
    }
}