package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.system.service.ISysDeptService;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "query_department", description = "查询企业部门信息，包括部门名称、部门领导、上级部门等")
@Component
public class DepartmentTool implements AiTool {

    private final ISysDeptService deptService;

    public DepartmentTool(ISysDeptService deptService) {
        this.deptService = deptService;
    }

    @Override
    public String execute(String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        String deptName = args.getString("dept_name");
        try {
            SysDept query = new SysDept();
            query.setDeptName(deptName);
            List<SysDept> depts = deptService.selectDeptList(query);
            if (depts.isEmpty()) {
                return "未找到名称包含「" + deptName + "」的部门。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(depts.size()).append(" 个部门：\n");
            for (SysDept d : depts) {
                sb.append("- 部门: ").append(d.getDeptName())
                  .append(", 负责人: ").append(d.getLeader())
                  .append(", 电话: ").append(d.getPhone())
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询部门信息失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject deptNameProp = new JSONObject();
        deptNameProp.put("type", "string");
        deptNameProp.put("description", "部门名称");

        JSONObject properties = new JSONObject();
        properties.put("dept_name", deptNameProp);

        schema.put("properties", properties);
        schema.put("required", List.of("dept_name"));
        return schema;
    }
}
