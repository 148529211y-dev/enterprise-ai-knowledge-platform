package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.BizProject;
import com.ruoyi.ai.mapper.BizProjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "query_project_status", description = "查询项目信息，包括项目名称、负责人、进度、状态、风险等级。当用户询问项目情况时使用。")
@Component
public class ProjectTool implements AiTool {

    private final BizProjectMapper bizProjectMapper;

    public ProjectTool(BizProjectMapper bizProjectMapper) {
        this.bizProjectMapper = bizProjectMapper;
    }

    @Override
    public String execute(String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        String projectName = args.getString("project_name");
        try {
            List<BizProject> projects = bizProjectMapper.selectByName(projectName);
            if (projects.isEmpty()) {
                return "未找到名称包含「" + projectName + "」的项目。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(projects.size()).append(" 个项目：\n");
            for (BizProject p : projects) {
                sb.append("- 项目名称: ").append(p.getProjectName())
                  .append(", 负责人: ").append(p.getManager())
                  .append(", 状态: ").append(p.getStatus())
                  .append(", 进度: ").append(p.getProgress()).append("%")
                  .append(", 风险: ").append(p.getRisk())
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询项目信息失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject nameProp = new JSONObject();
        nameProp.put("type", "string");
        nameProp.put("description", "项目名称");

        JSONObject properties = new JSONObject();
        properties.put("project_name", nameProp);

        schema.put("properties", properties);
        schema.put("required", List.of("project_name"));
        return schema;
    }
}
