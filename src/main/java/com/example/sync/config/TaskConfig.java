package com.example.sync.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class TaskConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // 스케줄러 풀 사이즈
        scheduler.setThreadNamePrefix("sync-task-");
        
        // Graceful Shutdown 설정
        // 어플리케이션 종료 시 실행 중인 작업이 완료될 때까지 대기
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        // 최대 대기 시간 (timeout-per-shutdown-phase 보다 작아야 함)
        scheduler.setAwaitTerminationSeconds(25);
        
        return scheduler;
    }
}
