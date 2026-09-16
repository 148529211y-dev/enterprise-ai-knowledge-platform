# 企业智能办公Agent平台

> 基于 RuoYi-Vue v3.9.2 二次开发，集成 RAG 知识问答、Agent 智能助手、Workflow 任务编排

## 项目背景

企业内部文档分散、查询效率低、重复咨询成本高。本平台在若依后台基础上，从"知识管理"升级为"智能办公Agent平台"：

```
用户登录 → 权限管理 → 知识库管理 → RAG问答 / Agent工具调用 / Workflow编排 → AI评估闭环
```

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 4.0 + Spring Security + MyBatis |
| 数据库 | MySQL 8.4 + Redis |
| AI调用 | OkHttp → Ollama (OpenAI兼容) |
| 文档解析 | Apache Tika 2.9 |
| 向量检索 | 内存向量 + 余弦相似度 |
| 前端 | Vue 2 + Element UI（沿用若依） |

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue + Element UI)                │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│              Spring Boot Application                     │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Controller Layer                                   │  │
│  │  AiController | KbController | WorkflowController  │  │
│  │  EvaluationController                              │  │
│  └───────────────────────┬───────────────────────────┘  │
│  ┌───────────────────────┴───────────────────────────┐  │
│  │ Service Layer (接口 + 实现)                         │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────────────────┐ │  │
│  │  │ 知识库   │ │ RAG     │ │ Agent Service       │ │  │
│  │  │ KbService│ │RagService│ │ ┌─────────────────┐│ │  │
│  │  └─────────┘ └─────────┘ │ │ Function Calling ││ │  │
│  │                           │ │ Multi-round Loop ││ │  │
│  │                           │ └─────────────────┘│ │  │
│  │                           └─────────────────────┘ │  │
│  └───────────────────────┬───────────────────────────┘  │
│  ┌───────────────────────┴───────────────────────────┐  │
│  │ Tool Layer (6个工具，自动注册)                       │  │
│  │  EmployeeTool | DepartmentTool | KnowledgeTool     │  │
│  │  ProjectTool | ApprovalTool | ReportTool           │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Workflow Layer (任务编排)                           │  │
│  │  WorkflowExecutor → WorkflowNode → 周报生成        │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Evaluation Layer (AI评估闭环)                      │  │
│  │  Feedback → Bad Case → 优化 → 效果提升             │  │
│  └───────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────┐  │
│  │ Data Layer                                         │  │
│  │  MySQL | Redis | 内存向量索引                       │  │
│  └───────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Agent 流程图

```
用户输入: "查询admin用户的信息"
  ↓
LLM 意图分析 + 工具选择
  ↓
tool_calls: [{name:"query_employee", arguments:{"name":"admin"}}]
  ↓
ToolRegistry.executeTool("query_employee", args)
  ↓
EmployeeTool → ISysUserService → 脱敏结果
  ↓
tool role 消息回传 LLM
  ↓
LLM 整合结果 → 最终答案
```

## Workflow 流程图

```
触发: "生成项目周报"
  ↓
StartNode → ProjectQueryNode → ReportGenerateNode → EndNode
  ↓            ↓                    ↓                  ↓
初始化      查询所有项目          LLM生成报告        完成保存
  ↓            ↓                    ↓                  ↓
wf_task     wf_task              wf_task            wf_instance
RUNNING     SUCCESS              SUCCESS            SUCCESS
```

## AI 评估闭环

```
用户提问 → AI回答 → 用户评分
  ↓
Bad Case 记录 → 失败原因分析
  ↓
Prompt/知识优化 → 效果提升
```

## Tool 体系

| Tool | 功能 | 触发示例 |
|------|------|----------|
| query_employee | 查询员工信息（脱敏） | "查询admin用户" |
| query_department | 查询部门信息 | "查询研发部" |
| search_knowledge | 知识库语义检索 | "公司请假制度" |
| query_project_status | 查询项目状态 | "智慧城市项目进度" |
| query_approval_status | 查询审批记录 | "张三的报销审批" |
| generate_project_report | 生成项目周报 | "生成项目周报" |

所有 Tool 通过 `@AgentTool` 注解自动注册，新增工具零修改。

## 数据库设计

| 表 | 说明 |
|----|------|
| kb_document / kb_chunk | 知识库文档和切片 |
| ai_conversation | 对话历史 |
| ai_audit_log | AI调用审计 |
| ai_tool_log | 工具调用日志 |
| ai_async_task | 异步任务状态 |
| biz_project | 项目信息 |
| biz_approval | 审批记录 |
| wf_instance / wf_task | 工作流实例和任务 |
| ai_feedback | 用户反馈 |
| ai_evaluation | AI评估记录 |
| ai_bad_case | Bad Case管理 |

## 快速启动

```bash
mysql -u root -e "CREATE DATABASE ruoyi DEFAULT CHARSET utf8mb4;"
mysql -u root ruoyi < sql/ry_20260320.sql
mysql -u root ruoyi < sql/quartz.sql
mysql -u root ruoyi < sql/ai_module.sql
mysql -u root ruoyi < sql/phase2_upgrade.sql

# 配置 application.yml 中 ai.provider/ai.chat-model
mvn clean install -DskipTests
mvn spring-boot:run -pl ruoyi-admin
```

## 项目规模

| 指标 | 数量 |
|------|------|
| Java文件 | 74 |
| 代码行数 | ~2900 |
| 数据库表 | 18（若依11 + AI模块7） |
| Tool数量 | 6 |
| 测试类 | 7 |
| 单元测试 | 39 |

## 当前局限

- 向量存储为内存实现，生产环境应替换 Qdrant
- 前端页面沿用若依，AI对话界面需单独开发
- 未提供 Dockerfile
- Workflow 当前仅支持周报生成，可扩展
