package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.BizProject;
import com.ruoyi.ai.mapper.BizProjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "generate_project_report", description = "生成项目周报，汇总项目状态、进度和风险信息。当用户要求生成报告或周报时使用。")
@Component
public class ReportTool implements AiTool {

    private final BizProjectMapper bizProjectMapper;

    public ReportTool(BizProjectMapper bizProjectMapper) {
        this.bizProjectMapper = bizProjectMapper;
    }

    @Override
    public String execute(String argsJson) {
        try {
            List<BizProject> projects = bizProjectMapper.selectAll();
            if (projects.isEmpty()) {
                return "当前没有项目数据，无法生成报告。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("【项目周报】\n");
            sb.append("项目总数: ").append(projects.size()).append("\n\n");
            int onTrack = 0;
            int atRisk = 0;
            for (BizProject p : projects) {
                sb.append("- ").append(p.getProjectName())
                  .append(" | 负责人: ").append(p.getManager())
                  .append(" | 状态: ").append(p.getStatus())
                  .append(" | 进度: ").append(p.getProgress()).append("%")
                  .append(" | 风险: ").append(p.getRisk())
                  .append("\n");
                if ("高".equals(p.getRisk()) || "严重".equals(p.getRisk())) {
                    atRisk++;
                } else {
                    onTrack++;
                }
            }
            sb.append("\n汇总: 正常推进 ").append(onTrack).append(" 个, 风险项目 ").append(atRisk).append(" 个");
            return sb.toString();
        } catch (Exception e) {
            return "生成项目报告失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", new JSONObject());
        return schema;
    }
}
