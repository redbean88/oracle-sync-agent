package com.example.sync.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
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
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isEmpty()) return hostname;
        String podName = System.getenv("POD_NAME");
        if (podName != null && !podName.isEmpty()) return podName;
        return "local-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
