package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.entity.BizApproval;
import com.ruoyi.ai.mapper.BizApprovalMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "query_approval_status", description = "查询审批记录，包括报销、请假、采购、出差等审批状态。当用户询问审批进度时使用。")
@Component
public class ApprovalTool implements AiTool {

    private final BizApprovalMapper bizApprovalMapper;

    public ApprovalTool(BizApprovalMapper bizApprovalMapper) {
        this.bizApprovalMapper = bizApprovalMapper;
    }

    @Override
    public String execute(String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        String applicant = args.getString("applicant");
        try {
            List<BizApproval> approvals = bizApprovalMapper.selectByApplicant(applicant);
            if (approvals.isEmpty()) {
                return "未找到申请人「" + applicant + "」的审批记录。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(approvals.size()).append(" 条审批记录：\n");
            for (BizApproval a : approvals) {
                sb.append("- 标题: ").append(a.getTitle())
                  .append(", 类型: ").append(a.getType())
                  .append(", 金额: ").append(a.getAmount())
                  .append(", 状态: ").append(a.getStatus())
                  .append(", 审批人: ").append(a.getApprover())
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询审批记录失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject applicantProp = new JSONObject();
        applicantProp.put("type", "string");
        applicantProp.put("description", "申请人姓名");

        JSONObject properties = new JSONObject();
        properties.put("applicant", applicantProp);

        schema.put("properties", properties);
        schema.put("required", List.of("applicant"));
        return schema;
    }
}
