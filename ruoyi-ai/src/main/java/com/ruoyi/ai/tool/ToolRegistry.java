package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.ai.service.VectorStoreService;
import com.ruoyi.ai.service.EmbeddingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Agent 工具注册中心 —— 管理所有可用工具
 *
 * 面试知识点 —— Agent = LLM + Memory + Tools
 *   Tools 是 Agent 能力的扩展：LLM 负责理解意图和推理，Tools 负责执行具体操作。
 *   通过 Function Calling 机制，LLM 可以"调用"外部函数，就像人使用工具一样。
 *
 * 工具定义格式遵循 OpenAI Function Calling 规范：
 *   { "type": "function", "function": { "name": "...", "description": "...", "parameters": {...} } }
 */
@Component
public class ToolRegistry {

    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final VectorStoreService vectorStore;
    private final EmbeddingService embeddingService;

    public ToolRegistry(ISysUserService userService, ISysDeptService deptService,
                        VectorStoreService vectorStore, EmbeddingService embeddingService) {
        this.userService = userService;
        this.deptService = deptService;
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
    }

    /**
     * 获取所有工具的 JSON 定义（传给 LLM 的 tools 参数）
     */
    public String getToolsJson() {
        return """
                [
                  {
                    "type": "function",
                    "function": {
                      "name": "query_employee",
                      "description": "根据员工姓名查询员工信息，包括所属部门、职位、邮箱等",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "name": {
                            "type": "string",
                            "description": "员工姓名"
                          }
                        },
                        "required": ["name"]
                      }
                    }
                  },
                  {
                    "type": "function",
                    "function": {
                      "name": "query_department",
                      "description": "查询企业部门信息，包括部门名称、部门领导、上级部门等",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "dept_name": {
                            "type": "string",
                            "description": "部门名称"
                          }
                        },
                        "required": ["dept_name"]
                      }
                    }
                  },
                  {
                    "type": "function",
                    "function": {
                      "name": "search_knowledge",
                      "description": "从企业知识库中搜索相关文档信息，当用户询问公司制度、流程、规范等问题时使用",
                      "parameters": {
                        "type": "object",
                        "properties": {
                          "query": {
                            "type": "string",
                            "description": "搜索关键词或问题"
                          }
                        },
                        "required": ["query"]
                      }
                    }
                  }
                ]
                """;
    }

    /**
     * 执行工具调用
     *
     * @param toolName 工具名称
     * @param argsJson 参数 JSON
     * @return 执行结果
     */
    public String executeTool(String toolName, String argsJson) {
        JSONObject args = JSONObject.parseObject(argsJson);
        switch (toolName) {
            case "query_employee":
                return queryEmployee(args.getString("name"));
            case "query_department":
                return queryDepartment(args.getString("dept_name"));
            case "search_knowledge":
                return searchKnowledge(args.getString("query"));
            default:
                return "未知工具: " + toolName;
        }
    }

    // ==================== 工具实现 ====================

    /**
     * Tool 1: 查询员工信息
     * 调用 RuoYi 原有的用户服务
     */
    private String queryEmployee(String name) {
        try {
            SysUser query = new SysUser();
            query.setUserName(name);
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
                  .append(", 部门ID: ").append(u.getDeptId())
                  .append(", 邮箱: ").append(u.getEmail())
                  .append(", 手机: ").append(u.getPhonenumber())
                  .append(", 状态: ").append(u.getStatus().equals("0") ? "正常" : "停用")
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询员工信息失败: " + e.getMessage();
        }
    }

    /**
     * Tool 2: 查询部门信息
     * 调用 RuoYi 原有的部门服务
     */
    private String queryDepartment(String deptName) {
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
                sb.append("- 部门名称: ").append(d.getDeptName())
                  .append(", 负责人: ").append(d.getLeader())
                  .append(", 联系电话: ").append(d.getPhone())
                  .append(", 状态: ").append(d.getStatus().equals("0") ? "正常" : "停用")
                  .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "查询部门信息失败: " + e.getMessage();
        }
    }

    /**
     * Tool 3: 知识库搜索
     * 复用 RAG 的向量检索能力
     */
    private String searchKnowledge(String query) {
        try {
            float[] queryVector = embeddingService.embed(query);
            if (queryVector.length == 0) {
                return "Embedding 服务不可用，无法搜索知识库。";
            }
            List<VectorStoreService.SearchResult> results = vectorStore.search(queryVector, 3);
            if (results.isEmpty()) {
                return "知识库中未找到与「" + query + "」相关的内容。";
            }
            StringBuilder sb = new StringBuilder("搜索到以下相关内容：\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append(i + 1).append(". ").append(results.get(i).content).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "知识库搜索失败: " + e.getMessage();
        }
    }
}
