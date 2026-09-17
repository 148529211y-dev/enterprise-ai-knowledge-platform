# 代码审查报告 — CODE_REVIEW.md

日期：2026-09-17

---

## 一、README 与代码一致性

| 声明 | README | 实际 | 一致？ |
|------|--------|------|--------|
| Java文件数 | 74 | 74 | ✅ |
| 代码行数 | ~2900 | 3443 | ⚠️ 应改为~3400 |
| Tool数量 | 6 | 6 | ✅ |
| 测试类 | 7 | 7 | ✅ |
| 数据库表 | 18 | 44 | ⚠️ 应改为44(含若依原表) |
| Spring Boot版本 | 4.0 | 4.0.3 | ✅ |
| Redis | 未声明版本 | 3.0.504 | ✅ |
| 异常体系 | 5个自定义异常 | 5个 | ✅ |
| Service接口 | 全部接口化 | 8/8 | ✅ |

**需修正：**
1. README中"约2900行"应改为"约3400行"
2. README中"18张数据库表"应改为"44张(若依11+AI模块7+业务6+其他20)"

---

## 二、Spring Boot 版本

- pom.xml: `<spring-boot.version>4.0.3</spring-boot.version>`
- 这是若依v3.9.2自带版本，非自选
- 面试说辞：若依团队在最新版本中升级到Spring Boot 4.x以支持Jakarta EE

---

## 三、Redis 版本

- 安装版本: Redis 3.0.504 (Microsoft Archive)
- 功能使用: 会话缓存(ai:chat:*)、登录Token
- 无版本相关问题

---

## 四、未使用代码检查

| 项目 | 状态 |
|------|------|
| AgentTool注解 | ✅ 仅用于标记，无冗余 |
| AiTool接口 | ✅ 被6个Tool实现 |
| ChunkStrategy枚举 | ⚠️ 定义了3种策略但代码中只用了PARAGRAPH |
| ConversationCacheService | ✅ 被Agent和RAG使用 |
| AiBadCaseMapper | ✅ 被EvaluationService使用 |

**发现：** ChunkStrategy枚举定义了PARAGRAPH/FIXED_SIZE/SENTENCE三种，但KbServiceImpl中硬编码了PARAGRAPH逻辑。应改为从RagConfig读取chunkStrategy并根据策略选择切片方式。

---

## 五、异常处理完整性

| 异常类 | 是否被Handler处理 | 是否被Service抛出 |
|--------|-------------------|-------------------|
| LlmCallException | ✅ | ✅ LlmServiceImpl |
| EmbeddingException | ✅ | ✅ EmbeddingServiceImpl |
| ToolExecutionException | ✅ | ✅ ToolRegistry |
| DocumentProcessException | ✅ | ⚠️ 定义但未直接抛出 |
| AiException | ✅ | ✅ 基类 |
| MethodArgumentNotValidException | ✅ | - |
| Exception(兜底) | ✅ | - |

**发现：** DocumentProcessException已定义但KbServiceImpl中直接抛出原始Exception。建议在Tika解析失败时抛出DocumentProcessException。

---

## 六、日志完整性

| Service | 日志数 | 评价 |
|---------|--------|------|
| AgentServiceImpl | 3 | 充分 |
| AsyncTaskServiceImpl | 3 | 充分 |
| ConversationCacheServiceImpl | 4 | 充分 |
| EmbeddingServiceImpl | 2 | 充分 |
| KbServiceImpl | 4 | 充分 |
| LlmServiceImpl | 6 | 充分 |
| RagServiceImpl | 1 | ⚠️ 偏少，建议增加召回和生成日志 |
| VectorStoreServiceImpl | 5 | 充分 |

**发现：** RagServiceImpl仅有1处日志，建议在向量召回和LLM生成环节增加日志。

---

## 七、Controller权限覆盖

| Controller | @PreAuthorize | try-catch |
|------------|---------------|-----------|
| AiController | ✅ ai:chat:use | ✅ |
| KbController | ✅ ai:kb:* | ✅ |
| EvaluationController | ❌ | ❌ |
| WorkflowController | ❌ | ❌ |

**发现：** EvaluationController和WorkflowController缺少权限控制和异常处理。

---

## 八、综合评价

**优点：**
- Service层全部接口化，面向接口编程
- 异常体系完整，5个自定义异常+全局处理器
- Tool自动注册机制设计良好
- 异步任务使用显式ThreadPoolExecutor

**需改进：**
1. README数据修正（行数、表数）
2. RagServiceImpl日志不足
3. EvaluationController/WorkflowController缺少权限控制
4. ChunkStrategy枚举未实际使用
5. DocumentProcessException未被主动抛出