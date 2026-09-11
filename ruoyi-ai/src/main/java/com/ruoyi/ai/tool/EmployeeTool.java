package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询员工信息工具
 */
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
                sb.append("- 姓名: ").append(u.getNickName())
                  .append(", 账号: ").append(u.getUserName())
                  .append(", 邮箱: ").append(u.getEmail())
                  .append(", 手机: ").append(u.getPhonenumber())
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询员工信息失败: " + e.getMessage();
        }
    }
}
