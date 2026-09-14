# Smoke Test Report — 2026-09-13

## 环境

- MySQL 8.4.9 ✅ (localhost:3306, database: ruoyi)
- Redis 3.0.504 ✅ (localhost:6379)
- Spring Boot 4.1.0 ✅ (localhost:8080)
- AI API Key: 未配置

## 测试结果

| # | 步骤 | 接口 | 状态码 | 结果 |
|---|------|------|--------|------|
| 1 | 登录 | POST /login | 200 | Token 203字符 |
| 2 | 用户信息 | GET /getInfo | 200 | 用户:若依, 角色:admin |
| 3 | 上传文档 | POST /ai/kb/upload | 200 | 返回 {docId:4, taskId:f9dafdb9...} |
| 4 | 查询任务状态 | GET /ai/kb/task/{taskId} | 200 | status=FAILED |
| 5 | 文档列表 | GET /ai/kb/list | 200 | 1条记录, status=待处理 |
| 6 | RAG问答 | POST /ai/chat (mode=rag) | 500 | EmbeddingException(code=401) |
| 7 | Agent调用 | POST /ai/chat (mode=agent) | 500 | LlmCallException(code=401) |

## 数据库验证

```sql
-- 审计日志：4条记录（每次AI调用都留痕）
SELECT id, operation_type, status, error_message FROM ai_audit_log;
-- 4 rows: rag/agent 调用全部记录，status=0，error_message=API调用失败

-- 异步任务：1条记录，状态机正确流转
SELECT task_id, status, progress FROM ai_async_task;
-- 1 row: f9dafdb9... → FAILED（因Embedding API 401）

-- 文档表：1条记录
SELECT id, title, status, chunk_count FROM kb_document;
-- 1 row: SmokeTest, status=0(待处理), chunks=null
```

## 分析

500 错误不是代码缺陷，是预期行为：

1. **Embedding API 返回 401** → `EmbeddingServiceImpl` 抛出 `EmbeddingException` → `AiExceptionHandler` 捕获 → 返回 `{code:500, msg:"Embedding API调用失败: code=401"}`
2. **LLM API 返回 401** → `LlmServiceImpl` 抛出 `LlmCallException` → `AiExceptionHandler` 捕获 → 返回 `{code:500, msg:"LLM API调用失败: code=401"}`

这正是 P1-4 整改的目标：异常不再被吞掉，统一由全局异常处理器处理。

## 需要 AI 同事解决的问题

1. **配置 API Key**：在 `application.yml` 中设置 `ai.api-key` 为有效的 DeepSeek API Key，或切换为 Ollama 本地模型
2. **Embedding 模型**：确认 DeepSeek 是否提供 `text-embedding-v3` 模型，如不提供需替换为兼容模型
3. **任务状态查询返回格式**：当前返回的是数据库 Entity，应返回统一的 JSON 格式
4. **RAG/Agent 无向量时的降级**：当知识库为空时，RAG 应返回友好提示而非 500
5. **Tool 日志表**：`ai_tool_log` 表存在但无数据（因为 Agent 调用在 LLM 阶段就失败了，未进入工具执行）
