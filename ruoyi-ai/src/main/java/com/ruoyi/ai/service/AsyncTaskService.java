package com.ruoyi.ai.service;

import com.ruoyi.ai.entity.AsyncTask;

public interface AsyncTaskService {

    String submitDocProcessTask(Long docId, Long userId, DocProcessor processor);

    AsyncTask getTaskStatus(String taskId);

    @FunctionalInterface
    interface DocProcessor {
        void process(Long docId) throws Exception;
    }
}
