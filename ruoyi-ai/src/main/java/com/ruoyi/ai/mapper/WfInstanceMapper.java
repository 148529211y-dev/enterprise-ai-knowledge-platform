package com.ruoyi.ai.mapper;

import com.ruoyi.ai.entity.WfInstance;
import org.apache.ibatis.annotations.Param;

public interface WfInstanceMapper {
    int insert(WfInstance instance);
    WfInstance selectByWorkflowId(String workflowId);
    int updateStatus(@Param("workflowId") String workflowId, @Param("status") String status, @Param("currentNode") String currentNode, @Param("contextJson") String contextJson);
}
