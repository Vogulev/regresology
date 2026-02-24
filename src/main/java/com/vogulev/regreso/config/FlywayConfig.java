package com.vogulev.regreso.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Spring Boot 4.0 убрал FlywayAutoConfiguration — Flyway больше не запускается
 * автоматически. Регистрируем бин вручную: Spring вызовет migrate() при старте
 * контекста, до того как поднимутся шедулеры и придут первые запросы.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }
}
