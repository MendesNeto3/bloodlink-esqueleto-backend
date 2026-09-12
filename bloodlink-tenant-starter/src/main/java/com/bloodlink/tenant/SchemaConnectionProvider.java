package com.bloodlink.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaConnectionProvider implements MultiTenantConnectionProvider <String> {

    private static final Logger log = LoggerFactory.getLogger(SchemaConnectionProvider.class);

    private final DataSource dataSource;

    public SchemaConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        TenantSchemaValidator.validarFormato(tenantIdentifier);

        Connection connection = getAnyConnection();
        try {
            executarSetSearchPath(connection, tenantIdentifier);
            return connection;
        } catch (SQLException e) {
            fecharSilenciosamente(connection);
            throw e;
        }
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            executarSetSearchPath(connection, "public");
        } finally {
            connection.close();
        }
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }

    private void executarSetSearchPath(Connection connection, String schema) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema + ", public");
        }
    }

    private void fecharSilenciosamente(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Falha ao fechar conexão após erro de SET search_path", e);
        }
    }
}
