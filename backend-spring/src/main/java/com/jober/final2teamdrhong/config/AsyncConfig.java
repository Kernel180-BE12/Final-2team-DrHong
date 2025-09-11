package com.jober.final2teamdrhong.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    // @Async가 사용할 실행기를 "가상 스레드 풀"로 지정
    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskExecutor() {
        // JDK 21 가상 스레드 per-task 실행기
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}