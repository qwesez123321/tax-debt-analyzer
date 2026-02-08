package ru.intelinfo.inczilla.mvc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.intelinfo.inczilla.mvc.io.ZipXmlDebtParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class DatasetUpdateService {
    private static final Logger log = LoggerFactory.getLogger(DatasetUpdateService.class);

    private final DebtStorageService storage;

    public DatasetUpdateService(DebtStorageService storage) {
        this.storage = storage;
    }

    public void updateIfRequired(LocalDate siteDate,
                                 ThrowingSupplier<Path> zipSupplier,
                                 ZipXmlDebtParser parser) {
        try {
            if (!storage.isUpdateRequired(siteDate)) {
                log.info("Данные актуальны => обновление не требуется");
                return;
            }

            log.info("Данные устарели/отсутствуют => загружаем новый набор (date={})", siteDate);

            Path zipPath = zipSupplier.get();
            try {
                storage.replaceAllAtomicallyFromZip(zipPath.toString(), siteDate, parser);
            } finally {
                try {
                    Files.deleteIfExists(zipPath);
                } catch (Exception ex) {
                    log.warn("Не удалось удалить временный архив: {}", zipPath, ex);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка обновления набора данных", e);
        }
    }
}