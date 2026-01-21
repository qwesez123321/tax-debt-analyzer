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

            if (!storage.isUpdateRequired(siteDate)) {
                log.info("Данные актуальны → обновление не требуется");
                return;
            }

            log.info("Данные устарели/отсутствуют → загружаем новый набор (date={})", siteDate);

            TaxDebtAnalyzer.ParsedArchive parsed = archiveSupplier.get();
            try {
                storage.replaceAllAtomically(parsed.companies, siteDate);
            } finally {
                try {
                    Files.deleteIfExists(parsed.zipPath);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить временный архив: {}", parsed.zipPath, ex);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления набора данных", e);
        }
    }
}