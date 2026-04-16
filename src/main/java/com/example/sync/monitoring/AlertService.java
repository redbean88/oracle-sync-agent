package com.example.sync.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertService {

    public void sendSlack(String message) {
        // 실제 구현(Slack API Client) 생략
        log.error("[SLACK ALERT] {}", message);
    }
}
