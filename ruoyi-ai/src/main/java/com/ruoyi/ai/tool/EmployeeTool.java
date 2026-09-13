package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.List;

@AgentTool(name = "query_employee", description = "根据姓名查询员工信息，包括所属部门、职位、邮箱等")
@Component
public class EmployeeTool implements AiTool {

    private final ISysUserService userService;

    public EmployeeTool(ISysUserService userService) {
        this.userService = userService;
    }

    @Override
    public String execute(String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        String name = args.getString("name");
        try {
            SysUser query = new SysUser();
            query.setNickName(name);
            List<SysUser> users = userService.selectUserList(query);
            if (users.isEmpty()) {
                return "未找到姓名包含「" + name + "」的员工。";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("找到 ").append(users.size()).append(" 条员工信息：\n");
            for (SysUser u : users) {
                String phone = u.getPhonenumber();
                String maskedPhone = (phone != null && phone.length() > 7)
                        ? phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4)
                        : "****";
                String email = u.getEmail();
                String maskedEmail;
                if (email != null && email.contains("@")) {
                    int atIdx = email.indexOf('@');
                    String prefix = atIdx > 3 ? email.substring(0, 3) : email.substring(0, atIdx);
                    maskedEmail = prefix + "***@" + email.substring(atIdx + 1);
                } else {
                    maskedEmail = "***";
                }
                sb.append("- 姓名: ").append(u.getNickName())
                  .append(", 账号: ").append(u.getUserName())
                  .append(", 邮箱: ").append(maskedEmail)
                  .append(", 手机: ").append(maskedPhone)
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询员工信息失败: " + e.getMessage();
        }
    }

    @Override
    public JSONObject getParametersSchema() {
        JSONObject schema = new JSONObject();
        schema.put("type", "object");

        JSONObject nameProp = new JSONObject();
        nameProp.put("type", "string");
        nameProp.put("description", "员工姓名");

        JSONObject properties = new JSONObject();
        properties.put("name", nameProp);

        schema.put("properties", properties);
        schema.put("required", List.of("name"));
        return schema;
    }
}
