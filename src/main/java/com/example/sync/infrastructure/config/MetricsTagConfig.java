package com.example.sync.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.ApplicationListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.MDC;

import java.util.UUID;

@Configuration
public class MetricsTagConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> instanceTagCustomizer() {
        String instance = resolveInstanceId();
        return registry -> registry.config().commonTags("instance", instance);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> instanceMdcInitializer() {
        return event -> MDC.put("instance", resolveInstanceId());
    }

    private static String resolveInstanceId() {
        return "local-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
