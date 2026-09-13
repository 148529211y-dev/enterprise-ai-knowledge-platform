# 企业智能知识管理与Agent辅助办公平台

> 基于 RuoYi-Vue v3.9.2 二次开发，集成 RAG 知识问答与 Agent 智能助手

## 项目背景

企业内部文档分散、查询效率低。本平台在若依后台基础上，新增 AI 智能模块：

```
用户登录 → 权限管理 → 知识库管理 → 文档异步解析 → 向量化 → RAG问答 / Agent工具调用
```

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 4.1 + Spring Security + MyBatis |
| 数据库 | MySQL 8.4 + Redis |
| AI调用 | OkHttp → DeepSeek/Ollama (OpenAI兼容) |
| 文档解析 | Apache Tika 2.9 |
| 向量检索 | 内存向量 + 余弦相似度（可切换 Qdrant） |
| 前端 | Vue 2 + Element UI（沿用若依） |

## 核心模块

### ruoyi-ai 模块（51个Java文件，~2200行）

```
├── config/          AiConfig, RagConfig, ChunkStrategy
├── controller/      AiController (chat/rag/agent), KbController (CRUD+异步上传+reindex)
├── service/         9个接口 + 8个实现
├── tool/            @AgentTool注解 + AiTool接口 + 3个工具实现
├── entity/          6个实体
├── mapper/          6个Mapper接口 + 6个XML
├── domain/          DTO/VO/Query
├── exception/       5个自定义异常 + 全局异常处理器
└── test/            7个测试类，39个测试方法
```

### 功能清单

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户权限管理 | 若依原生 | RBAC + JWT |
| 知识库文档管理 | ✅ | 上传/删除/列表/详情 |
| 文档异步处理 | ✅ | ThreadPoolExecutor，状态机 WAITING→RUNNING→SUCCESS/FAILED |
| 文档解析 | ✅ | Apache Tika (PDF/Word/MD) |
| 文本切片 | ✅ | 按段落+最大500字+50字重叠 |
| Embedding向量化 | ✅ | 调用DeepSeek/Ollama Embedding API |
| 向量检索 | ✅ | 内存向量+余弦相似度，启动时从MySQL恢复 |
| RAG知识问答 | ✅ | Query Embedding→向量召回→Prompt→LLM生成 |
| Agent智能助手 | ✅ | Function Calling循环，最多5轮 |
| 工具自动注册 | ✅ | @AgentTool注解扫描，零修改扩展 |
| 工具列表 | ✅ | query_employee, query_department, search_knowledge |
| 会话管理 | ✅ | Redis短期缓存+MySQL长期存储，双层读写 |
| AI审计日志 | ✅ | ai_audit_log 记录每次AI调用 |
| 工具调用日志 | ✅ | ai_tool_log 记录每次工具调用 |
| 异步任务管理 | ✅ | ai_async_task 跟踪文档处理状态 |
| RBAC权限控制 | ✅ | @PreAuthorize on AI endpoints |
| 参数校验 | ✅ | @Valid + JSR380 |
| 全局异常处理 | ✅ | AiExceptionHandler 统一拦截 |
| 相似度阈值过滤 | ✅ | RagConfig.similarityThreshold |
| 向量索引恢复 | ✅ | 启动时从MySQL加载切片元数据 |
| 手动重建索引 | ✅ | POST /ai/kb/reindex |
| 敏感字段脱敏 | ✅ | 员工手机号/邮箱脱敏 |

### Agent Function Calling 流程

```
用户消息 + 工具定义 → LLM
  ↓ LLM返回 tool_calls
执行工具 → 结果通过 tool role 传回 LLM
  ↓ LLM整合结果
生成最终答案（最多循环5轮）
```

消息格式符合 OpenAI-compatible 协议：`ChatMessage` DTO 保留 role/content/tool_calls/tool_call_id。

### RAG 流程

```
用户提问 → Embedding → 向量检索 TopK → 相似度过滤 → Prompt构造 → LLM回答 + 引用
```

参数通过 `RagConfig` 外部化配置：topK、temperature、similarityThreshold、maxTokens。

## 数据库表

| 表 | 说明 |
|----|------|
| kb_document | 知识库文档 |
| kb_chunk | 文档切片 |
| ai_conversation | 对话历史 |
| ai_audit_log | AI调用审计 |
| ai_tool_log | 工具调用日志 |
| ai_async_task | 异步任务状态 |

## 快速启动

```bash
# 数据库
mysql -u root -e "CREATE DATABASE ruoyi DEFAULT CHARSET utf8mb4;"
mysql -u root ruoyi < sql/ry_20260320.sql
mysql -u root ruoyi < sql/quartz.sql
mysql -u root ruoyi < sql/ai_module.sql

# 配置 application.yml 中 ai.api-key

# 后端
mvn clean install -DskipTests
mvn spring-boot:run -pl ruoyi-admin

# 前端
cd ruoyi-ui && npm install && npm run dev
```

## 工具扩展

新增工具只需两步，无需修改任何已有代码：

```java
@AgentTool(name = "my_tool", description = "工具描述")
@Component
public class MyTool implements AiTool {
    @Override
    public String execute(String argsJson) { ... }
    @Override
    public JSONObject getParametersSchema() { ... }
}
```

## 当前局限

- 向量存储为内存实现，重启后需通过 reindex 重建（切片元数据从MySQL恢复）
- 生产环境应替换为 Qdrant/Milvus
- 前端页面沿用若依，AI对话界面需单独开发
- 未提供 Dockerfile / docker-compose

## License

基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) (MIT License) 二次开发。
