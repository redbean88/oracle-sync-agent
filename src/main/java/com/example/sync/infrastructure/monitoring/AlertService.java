package com.example.sync.infrastructure.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    public void sendAlert(String message) {
        // 슬랙 기능 제거, 로그 출력으로 대체
        log.error("[ALERT] {}", message);
    }
}
