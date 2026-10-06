package com.bloodlink.tenant;

import org.hibernate.cfg.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@AutoConfiguration
public class TenantAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> filterRegistrationBean() {
        FilterRegistrationBean<CorrelationIdFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new CorrelationIdFilter());
        registrationBean.setOrder(0);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<UserRoleContextFilter> userRoleContextFilter() {
        FilterRegistrationBean<UserRoleContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new UserRoleContextFilter());
        registrationBean.setOrder(0);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<TenantResolverFilter> tenantResolverFilter() {
        FilterRegistrationBean<TenantResolverFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TenantResolverFilter());
        registrationBean.setOrder(1);
        return registrationBean;
    }

    @Bean
    public TenantIdentifierResolver tenantIdentifierResolver() {
        return new TenantIdentifierResolver();
    }

    @Bean
    public SchemaConnectionProvider schemaConnectionProvider(DataSource dataSource) {
        return new SchemaConnectionProvider(dataSource);
    }

    @Bean
    public FlywaySchemaProvisionerAdapter flywaySchemaProvisionerAdapter(
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            @Value("${bloodlink.tenant.migrations-location:classpath:db/migration/tenant}") String migrationsLocation) {
        return new FlywaySchemaProvisionerAdapter(dataSource, jdbcTemplate, migrationsLocation);
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
            SchemaConnectionProvider schemaConnectionProvider,
            TenantIdentifierResolver tenantIdentifierResolver) {
        return hibernateProperties -> {
            hibernateProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, schemaConnectionProvider);
            hibernateProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
        };
    }
}
