package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.AsyncTask;

/**
 * 异步任务 Mapper
 */
public interface AsyncTaskMapper {
    int insert(AsyncTask task);
    AsyncTask selectByTaskId(String taskId);
    int updateStatus(String taskId, String status, String progress, String errorMessage);
}
