package ru.intelinfo.inczilla.mvc.service;

import ru.intelinfo.inczilla.mvc.db.DatabaseManager;
import ru.intelinfo.inczilla.mvc.repository.CompanyDebtRepository;
import ru.intelinfo.inczilla.mvc.repository.DatasetMetaRepository;


public class DbSchemaService {

    private final DatabaseManager db;
    private final DatasetMetaRepository metaRepo;
    private final CompanyDebtRepository debtRepo;

    public DbSchemaService(DatabaseManager db,
                           DatasetMetaRepository metaRepo,
                           CompanyDebtRepository debtRepo) {
        this.db = db;
        this.metaRepo = metaRepo;
        this.debtRepo = debtRepo;
    }

    public void init() {
        db.withConnectionSql(conn -> {
            metaRepo.initSchema(conn);
            debtRepo.initSchema(conn);
            debtRepo.initStageSchema(conn);
            return null;
        });
    }
}