package com.ruoyi.ai.service.workflow;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.BizProject;
import com.ruoyi.ai.mapper.BizProjectMapper;
import com.ruoyi.ai.service.LlmService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeeklyReportWorkflow {

    private final BizProjectMapper bizProjectMapper;
    private final LlmService llmService;
    private final WorkflowExecutor workflowExecutor;

    public WeeklyReportWorkflow(BizProjectMapper bizProjectMapper, LlmService llmService, WorkflowExecutor workflowExecutor) {
        this.bizProjectMapper = bizProjectMapper;
        this.llmService = llmService;
        this.workflowExecutor = workflowExecutor;
    }

    public String execute(Long userId) {
        List<WorkflowNode> nodes = new ArrayList<>();

        nodes.add(new WorkflowNode() {
            @Override
            public String getName() {
                return "StartNode";
            }

            @Override
            public Map<String, Object> execute(Map<String, Object> context) throws Exception {
                return new HashMap<>();
            }
        });

        nodes.add(new WorkflowNode() {
            @Override
            public String getName() {
                return "ProjectQueryNode";
            }

            @Override
            public Map<String, Object> execute(Map<String, Object> context) throws Exception {
                List<BizProject> projects = bizProjectMapper.selectAll();
                Map<String, Object> result = new HashMap<>();
                result.put("projects", projects);
                return result;
            }
        });

        nodes.add(new WorkflowNode() {
            @Override
            public String getName() {
                return "ReportGenerateNode";
            }

            @Override
            public Map<String, Object> execute(Map<String, Object> context) throws Exception {
                List<BizProject> projects = (List<BizProject>) context.get("projects");
                StringBuilder projectInfo = new StringBuilder();
                if (projects != null) {
                    for (BizProject p : projects) {
                        projectInfo.append("项目: ").append(p.getProjectName())
                            .append(", 负责人: ").append(p.getManager())
                            .append(", 状态: ").append(p.getStatus())
                            .append(", 进度: ").append(p.getProgress()).append("%")
                            .append(", 风险: ").append(p.getRisk())
                            .append("; ");
                    }
                }
                String report = llmService.chat(
                    "你是一个项目经理助手，请根据以下项目数据生成一份简洁的项目周报。",
                    "以下是本周各项目数据：" + projectInfo.toString() + "请生成周报摘要。"
                );
                Map<String, Object> result = new HashMap<>();
                result.put("report", report);
                return result;
            }
        });

        return workflowExecutor.executeWorkflow("WEEKLY_REPORT", nodes, userId);
    }
}
