package com.bloodlink.tenant;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class FlywaySchemaProvisionerAdapter implements SchemaProvisionerPort{

    private static final Logger log = LoggerFactory.getLogger(FlywaySchemaProvisionerAdapter.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final String migrationsLocation;

    public FlywaySchemaProvisionerAdapter(DataSource dataSource, JdbcTemplate jdbcTemplate, String migrationsLocation) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.migrationsLocation = migrationsLocation;
    }

    @Override
    public void provisionar(String schema) {
        TenantSchemaValidator.validarFormatoProvisionamento(schema); // antes: validarFormato(schema)

        log.info("Provisionando schema: {}", schema);
        jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schema));

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations(migrationsLocation)
                .load()
                .migrate();

        log.info("Schema provisionado com sucesso: {}", schema);
    }
}
