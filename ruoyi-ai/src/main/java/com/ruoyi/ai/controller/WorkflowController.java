package com.ruoyi.ai.controller;

import com.ruoyi.ai.service.workflow.WeeklyReportWorkflow;
import com.ruoyi.ai.service.workflow.WorkflowExecutor;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ai/workflow")
public class WorkflowController extends BaseController {

    private final WeeklyReportWorkflow weeklyReportWorkflow;
    private final WorkflowExecutor workflowExecutor;

    public WorkflowController(WeeklyReportWorkflow weeklyReportWorkflow, WorkflowExecutor workflowExecutor) {
        this.weeklyReportWorkflow = weeklyReportWorkflow;
        this.workflowExecutor = workflowExecutor;
    }

    @PostMapping("/weekly-report")
    public AjaxResult triggerWeeklyReport() {
        Long userId = SecurityUtils.getUserId();
        String workflowId = weeklyReportWorkflow.execute(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("workflowId", workflowId);
        return AjaxResult.success(data);
    }

    @GetMapping("/status/{workflowId}")
    public AjaxResult getWorkflowStatus(@PathVariable String workflowId) {
        return AjaxResult.success(workflowExecutor.getWorkflowStatus(workflowId));
    }
}
