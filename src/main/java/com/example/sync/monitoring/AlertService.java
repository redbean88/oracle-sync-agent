package com.example.sync.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    public void sendSlack(String message) {
        log.error("[SLACK ALERT] {}", message);
    }
}
