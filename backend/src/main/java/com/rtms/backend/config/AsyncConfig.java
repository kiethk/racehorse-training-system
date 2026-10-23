package com.rtms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "applicationTaskExecutor")
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        // 1. Cấu hình ThreadPool tiêu chuẩn
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("rtms-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        // 2. Bọc lại bằng DelegatingSecurityContextAsyncTaskExecutor để truyền SecurityContext
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationTaskExecutor();
    }
}