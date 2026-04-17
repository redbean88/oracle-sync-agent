package com.example.sync.base.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateProperties;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateSettings;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.sync.repository.proxy",
        entityManagerFactoryRef = "proxyEntityManager",
        transactionManagerRef = "proxyTransactionManager"
)
public class ProxyJpaConfig {

    private final JpaProperties jpaProperties;
    private final HibernateProperties hibernateProperties;

    public ProxyJpaConfig(JpaProperties jpaProperties, HibernateProperties hibernateProperties) {
        this.jpaProperties = jpaProperties;
        this.hibernateProperties = hibernateProperties;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean proxyEntityManager(
            EntityManagerFactoryBuilder builder,
            @Qualifier("proxyDataSource") DataSource dataSource) {

        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(), new HibernateSettings());

        return builder
                .dataSource(dataSource)
                .packages("com.example.sync.domain.proxy")
                .persistenceUnit("proxy")
                .properties(properties)
                .build();
    }

    @Bean
    public PlatformTransactionManager proxyTransactionManager(
            @Qualifier("proxyEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }
}
