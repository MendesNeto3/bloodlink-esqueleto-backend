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

    public FlywaySchemaProvisionerAdapter(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void provisionar(String schema) {
        TenantSchemaValidator.validarFormato(schema);

        log.info("Schema provisioned: {}", schema);
        jdbcTemplate.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s",schema));

        Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("classpath:/public/schemas")
                .load()
                .migrate();

        log.info("Schema provisioned: {}", schema);
    }
}
