package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.WfTask;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface WfTaskMapper {
    int insert(WfTask task);
    int updateResult(@Param("id") Long id, @Param("status") String status, @Param("outputJson") String outputJson, @Param("errorMessage") String errorMessage, @Param("durationMs") Integer durationMs);
    List<WfTask> selectByWorkflowId(String workflowId);
}
