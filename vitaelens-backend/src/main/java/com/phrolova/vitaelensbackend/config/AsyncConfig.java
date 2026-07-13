package com.phrolova.vitaelensbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数。线程池中始终保持存活的线程
        executor.setCorePoolSize(2);
        // 最大线程数。线程池中允许创建的最大线程数
        executor.setMaxPoolSize(4);
        // 工作队列的大小。用于存放等待执行的任务
        executor.setQueueCapacity(20);
        // 设定线程命名前缀。如analysis-1、analysis-2
        executor.setThreadNamePrefix("analysis-");
        // 当线程池和队列满时，采取处理策略：由调用者线程直接执行该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 应用关闭时，等待所有已提交的任务执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();

        return executor;
    }
}
