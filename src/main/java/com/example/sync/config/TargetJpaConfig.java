package com.example.sync.config;

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
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.sql.OracleTemplates;
import com.querydsl.sql.SQLQueryFactory;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.sync.repository.target",
        entityManagerFactoryRef = "targetEntityManager",
        transactionManagerRef = "targetTransactionManager"
)
public class TargetJpaConfig {

    private final JpaProperties jpaProperties;
    private final HibernateProperties hibernateProperties;

    // Use Constructor Injection (No Lombok)
    public TargetJpaConfig(JpaProperties jpaProperties, HibernateProperties hibernateProperties) {
        this.jpaProperties = jpaProperties;
        this.hibernateProperties = hibernateProperties;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean targetEntityManager(
            EntityManagerFactoryBuilder builder, 
            @Qualifier("targetDataSource") DataSource dataSource) {
            
        Map<String, Object> properties = hibernateProperties.determineHibernateProperties(
                jpaProperties.getProperties(), new HibernateSettings());

        return builder
                .dataSource(dataSource)
                .packages("com.example.sync.domain.target")
                .persistenceUnit("target")
                .properties(properties)
                .build();
    }

    @Bean
    public PlatformTransactionManager targetTransactionManager(
            @Qualifier("targetEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }

    @Bean
    public JPAQueryFactory targetQueryFactory(@Qualifier("targetEntityManager") EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    public SQLQueryFactory targetSqlQueryFactory(@Qualifier("targetDataSource") DataSource dataSource) {
        com.querydsl.sql.SQLTemplates templates = com.querydsl.sql.OracleTemplates.builder().build();
        com.querydsl.sql.Configuration configuration = new com.querydsl.sql.Configuration(templates);
        return new SQLQueryFactory(configuration, dataSource);
    }
}
