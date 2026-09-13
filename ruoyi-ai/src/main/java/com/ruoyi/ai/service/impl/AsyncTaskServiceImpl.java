package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.AsyncTask;
import com.ruoyi.ai.mapper.AsyncTaskMapper;
import com.ruoyi.ai.service.AsyncTaskService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncTaskServiceImpl implements AsyncTaskService {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);

    private final AsyncTaskMapper asyncTaskMapper;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(20),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public AsyncTaskServiceImpl(AsyncTaskMapper asyncTaskMapper) {
        this.asyncTaskMapper = asyncTaskMapper;
    }

    @Override
    public String submitDocProcessTask(Long docId, Long userId, DocProcessor processor) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        AsyncTask task = new AsyncTask();
        task.setTaskId(taskId);
        task.setTaskType("doc_process");
        task.setBizId(docId);
        task.setStatus("WAITING");
        task.setProgress("任务已创建，等待执行");
        task.setCreateBy(userId);
        asyncTaskMapper.insert(task);

        CompletableFuture.runAsync(() -> {
            asyncTaskMapper.updateStatus(taskId, "RUNNING", "正在处理文档...", null);
            log.info("异步任务开始: taskId={}, docId={}", taskId, docId);

            try {
                processor.process(docId);
                asyncTaskMapper.updateStatus(taskId, "SUCCESS", "处理完成", null);
                log.info("异步任务成功: taskId={}", taskId);
            } catch (Exception e) {
                asyncTaskMapper.updateStatus(taskId, "FAILED", "处理失败", e.getMessage());
                log.error("异步任务失败: taskId={}", taskId, e);
            }
        }, executor);

        return taskId;
    }

    @Override
    public AsyncTask getTaskStatus(String taskId) {
        return asyncTaskMapper.selectByTaskId(taskId);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
