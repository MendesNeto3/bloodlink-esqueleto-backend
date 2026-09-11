package com.bloodlink.tenant;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;

import java.sql.Connection;
import java.sql.SQLException;

public class SchemaConnectionProvider implements MultiTenantConnectionProvider <String> {


    @Override
    public Connection getAnyConnection() throws SQLException {
        return null;
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {

    }

    @Override
    public Connection getConnection(String s) throws SQLException {
        return null;
    }

    @Override
    public void releaseConnection(String s, Connection connection) throws SQLException {

    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    public boolean isUnwrappableAs(@org.checkerframework.checker.nullness.qual.UnknownKeyFor @org.checkerframework.checker.nullness.qual.NonNull @org.checkerframework.checker.initialization.qual.Initialized Class<@org.checkerframework.checker.nullness.qual.UnknownKeyFor @org.checkerframework.checker.nullness.qual.NonNull @org.checkerframework.checker.initialization.qual.Initialized ?> aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(@org.checkerframework.checker.nullness.qual.UnknownKeyFor @org.checkerframework.checker.nullness.qual.NonNull @org.checkerframework.checker.initialization.qual.Initialized Class<T> aClass) {
        return null;
    }
}
