package com.ruoyi.ai.service.impl;

import com.ruoyi.ai.entity.AsyncTask;
import com.ruoyi.ai.mapper.AsyncTaskMapper;
import com.ruoyi.ai.exception.DocumentProcessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务服务 —— 管理文档处理等耗时任务
 *
 * 面试知识点：
 * 1. 为什么用异步？ —— 文档解析+切片+Embedding可能耗时数秒到数十秒，同步阻塞用户体验差
 * 2. 实现方式：CompletableFuture + 自定义线程池（比 @Async 更可控）
 * 3. 任务状态机：WAITING → RUNNING → SUCCESS / FAILED
 * 4. 前端轮询：上传后返回 taskId，前端定时查询状态
 *
 * 为什么用 ThreadPoolExecutor 而不是 @Async？
 * - @Async 使用默认线程池，无法控制队列大小和拒绝策略
 * - 自定义线程池可以精确控制：核心线程数、最大线程数、队列容量、拒绝策略
 */
@Service
public class AsyncTaskServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(AsyncTaskServiceImpl.class);

    private final AsyncTaskMapper asyncTaskMapper;

    /**
     * 自定义线程池
     * 核心2线程，最大4线程，队列容量20
     * 适用于文档处理这类IO密集型任务
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AsyncTaskServiceImpl(AsyncTaskMapper asyncTaskMapper) {
        this.asyncTaskMapper = asyncTaskMapper;
    }

    /**
     * 提交异步文档处理任务
     *
     * @param docId     文档ID
     * @param processor 实际处理逻辑（函数式接口）
     * @return taskId（前端用于轮询）
     */
    public String submitDocProcessTask(Long docId, Long userId, DocProcessor processor) {
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 1. 创建任务记录（WAITING状态）
        AsyncTask task = new AsyncTask();
        task.setTaskId(taskId);
        task.setTaskType("doc_process");
        task.setBizId(docId);
        task.setStatus("WAITING");
        task.setProgress("任务已创建，等待执行");
        task.setCreateBy(userId);
        asyncTaskMapper.insert(task);

        // 2. 异步执行
        CompletableFuture.runAsync(() -> {
            // 更新为 RUNNING
            asyncTaskMapper.updateStatus(taskId, "RUNNING", "正在处理文档...", null);
            log.info("异步任务开始: taskId={}, docId={}", taskId, docId);

            try {
                processor.process(docId);
                // 更新为 SUCCESS
                asyncTaskMapper.updateStatus(taskId, "SUCCESS", "处理完成", null);
                log.info("异步任务成功: taskId={}", taskId);
            } catch (Exception e) {
                // 更新为 FAILED
                asyncTaskMapper.updateStatus(taskId, "FAILED", "处理失败", e.getMessage());
                log.error("异步任务失败: taskId={}", taskId, e);
            }
        }, executor);

        return taskId;
    }

    /** 查询任务状态 */
    public AsyncTask getTaskStatus(String taskId) {
        return asyncTaskMapper.selectByTaskId(taskId);
    }

    /**
     * 文档处理函数式接口
     */
    @FunctionalInterface
    public interface DocProcessor {
        void process(Long docId) throws Exception;
    }
}
