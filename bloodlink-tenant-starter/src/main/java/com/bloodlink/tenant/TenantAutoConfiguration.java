package com.bloodlink.tenant;

import org.springframework.boot.autoconfigure.AutoConfiguration;
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
    public FlywaySchemaProvisionerAdapter flywaySchemaProvisionerAdapter(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        return new FlywaySchemaProvisionerAdapter(dataSource, jdbcTemplate);
    }
}
