package com.ruoyi.ai.service.workflow;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.WfInstance;
import com.ruoyi.ai.entity.WfTask;
import com.ruoyi.ai.mapper.WfInstanceMapper;
import com.ruoyi.ai.mapper.WfTaskMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowExecutor {

    private final WfInstanceMapper wfInstanceMapper;
    private final WfTaskMapper wfTaskMapper;

    public WorkflowExecutor(WfInstanceMapper wfInstanceMapper, WfTaskMapper wfTaskMapper) {
        this.wfInstanceMapper = wfInstanceMapper;
        this.wfTaskMapper = wfTaskMapper;
    }

    public String executeWorkflow(String workflowType, List<WorkflowNode> nodes, Long userId) {
        String workflowId = UUID.randomUUID().toString().replace("-", "");
        WfInstance instance = new WfInstance();
        instance.setWorkflowId(workflowId);
        instance.setWorkflowType(workflowType);
        instance.setStatus("RUNNING");
        instance.setCurrentNode(nodes.isEmpty() ? "" : nodes.get(0).getName());
        instance.setContextJson("{}");
        instance.setCreateBy(userId);
        wfInstanceMapper.insert(instance);

        Map<String, Object> context = new java.util.HashMap<>();
        boolean failed = false;

        for (WorkflowNode node : nodes) {
            WfTask task = new WfTask();
            task.setWorkflowId(workflowId);
            task.setNodeName(node.getName());
            task.setStatus("RUNNING");
            task.setInputJson(JSONObject.toJSONString(context));
            wfTaskMapper.insert(task);

            long start = System.currentTimeMillis();
            try {
                Map<String, Object> result = node.execute(context);
                if (result != null) {
                    context.putAll(result);
                }
                long duration = System.currentTimeMillis() - start;
                wfTaskMapper.updateResult(task.getId(), "SUCCESS", JSONObject.toJSONString(context), null, (int) duration);
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - start;
                wfTaskMapper.updateResult(task.getId(), "FAILED", null, e.getMessage(), (int) duration);
                failed = true;
                break;
            }
        }

        if (failed) {
            wfInstanceMapper.updateStatus(workflowId, "FAILED", "", JSONObject.toJSONString(context));
        } else {
            wfInstanceMapper.updateStatus(workflowId, "SUCCESS", "", JSONObject.toJSONString(context));
        }

        return workflowId;
    }

    public WfInstance getWorkflowStatus(String workflowId) {
        return wfInstanceMapper.selectByWorkflowId(workflowId);
    }
}
