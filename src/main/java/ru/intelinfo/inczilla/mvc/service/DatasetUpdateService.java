package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.time.LocalDate;

public class DatasetUpdateService {
    private static final Logger log = LoggerFactory.getLogger(DatasetUpdateService.class);

    private final DebtStorageService storage;

    public DatasetUpdateService(DebtStorageService storage) {
        this.storage = storage;
    }

    public void updateIfRequired(LocalDate siteDate,
                                 ThrowingSupplier<TaxDebtAnalyzer.ParsedArchive> archiveSupplier) {
        try {
            storage.ensureSchema();

            if (!storage.isUpdateRequired(siteDate)) {
                log.info("Данные в БД актуальны → обновление не требуется");
                return;
            }

            log.info("Данные в БД отсутствуют или устарели → обновляем");

            TaxDebtAnalyzer.ParsedArchive parsed = archiveSupplier.get();
            try {
                storage.replaceAllAtomically(parsed.companies, siteDate);
                log.info("Обновление завершено успешно. Дата набора: {}", siteDate);
            } finally {
                try {
                    Files.deleteIfExists(parsed.zipPath);
                    log.info("Временный архив удалён: {}", parsed.zipPath);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить временный архив: {}", parsed.zipPath, ex);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления набора данных", e);
        }
    }
}