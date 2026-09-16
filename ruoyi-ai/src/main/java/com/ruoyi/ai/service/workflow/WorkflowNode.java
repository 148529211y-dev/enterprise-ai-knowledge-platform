package com.ruoyi.ai.service.workflow;

import java.util.Map;

public interface WorkflowNode {
    String getName();
    Map<String, Object> execute(Map<String, Object> context) throws Exception;
}
