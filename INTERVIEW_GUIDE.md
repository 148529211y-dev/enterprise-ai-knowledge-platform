# 面试指南 — INTERVIEW_GUIDE.md

> 所有内容基于项目真实代码，可直接用于面试准备

---

## 一、项目整体介绍（3分钟版）

### 开场（30秒）
> 我的项目是"企业智能知识管理与Agent辅助办公平台"，基于若依企业后台管理系统二次开发。
>
> 背景是企业内部文档分散、查询效率低、重复咨询成本高。我在若依的用户权限基础上，新增了知识库管理、RAG智能问答、Agent智能助手三个核心模块。

### 架构（40秒）
> 系统分两层：
>
> **基础层**是若依提供的：用户管理、角色权限、JWT认证、操作日志，这块是成熟的我没有改动。
>
> **AI层**是我自主开发的 `ruoyi-ai` 模块，核心有三个能力：
> - 知识库管理：用户上传文档，系统自动解析、切片、向量化
> - RAG问答：先在知识库检索相关文档，再让LLM基于文档回答
> - Agent助手：LLM可以自动调用工具，比如查员工、查部门、搜知识库

### 技术亮点（60秒）
> 说三个技术亮点：
>
> **第一，不依赖AI框架。** 我直接用OkHttp调用DeepSeek的OpenAI兼容API，自己实现Embedding、余弦相似度检索、Prompt构造。好处是完全掌控请求响应细节，面试时能讲清楚每一层的原理。
>
> **第二，Agent的Function Calling。** 我设计了`@AgentTool`注解+`AiTool`接口的工具扩展机制。新增工具只需实现接口加注解，不需要修改任何已有代码，符合开闭原则。工具调用是一个循环：LLM返回tool_calls→执行工具→结果喂回LLM→最多循环5轮。
>
> **第三，工程化设计。** Service层全部面向接口编程，有完整的自定义异常体系和全局异常处理器。文档处理用异步任务，上传后立即返回taskId，前端轮询状态。所有AI调用记录审计日志，满足央国企合规要求。

### 收获（30秒）
> 通过这个项目，我深入理解了Spring Boot自动配置、MyBatis Mapper扫描、RBAC权限模型，以及RAG、Embedding、Function Calling等AI核心概念。

---

## 二、技术难点

### 难点1：RAG的切片策略选择

**问题：** 文档切片太大，检索结果不精准；切片太小，丢失上下文。

**解决方案：** 采用"按段落 + 最大长度限制 + 重叠窗口"的混合策略。
- 优先按段落（`\n\n`）切分，保持语义完整性
- 超长段落按500字再切
- 相邻切片有50字重叠，保证上下文连贯

**代码位置：** `KbServiceImpl.chunkText()`

### 难点2：Agent工具调用的循环控制

**问题：** LLM可能无限调用工具，或者调用错误的工具。

**解决方案：**
1. 设置最大轮次 `MAX_TOOL_ROUNDS = 5`
2. 工具执行设超时
3. 工具返回错误信息而非抛异常，让LLM能优雅降级
4. 每次工具调用都记录日志

**代码位置：** `AgentServiceImpl.chat()` 中的 for 循环

### 难点3：Service接口拆分

**问题：** 原来所有Service都是直接的实现类，不利于测试和扩展。

**解决方案：** 拆分为接口+实现：
- `LlmService`(接口) + `LlmServiceImpl`(实现)
- Controller 注入接口类型，Spring 自动注入实现
- 好处：可以 Mock 测试，可以替换实现（如切换到 Spring AI）

### 难点4：Redis + MySQL 双层缓存

**问题：** 对话历史频繁读写，只用MySQL性能不够。

**解决方案：**
- Redis 存短期上下文（最近20轮，TTL 2小时）
- MySQL 存全量历史（永久保存）
- 读取优先Redis，未命中时从MySQL加载并回填

**代码位置：** `ConversationCacheServiceImpl`

---

## 三、Java 相关问题

### Q: HashMap 的底层原理？
> **答：** 数组+链表+红黑树。默认容量16，负载因子0.75。链表长度≥8且数组长度≥64时转红黑树。扩容时容量翻倍，重新hash。
> **项目关联：** VectorStoreServiceImpl 用 ConcurrentHashMap 存储向量索引，线程安全。

### Q: ConcurrentHashMap 和 HashMap 的区别？
> **答：** ConcurrentHashMap 线程安全。JDK8后用 CAS + synchronized（锁单个Node），不再用分段锁。
> **项目关联：** 向量索引和切片缓存使用 ConcurrentHashMap，因为可能有并发写入。

### Q: volatile 关键字的作用？
> **答：** 保证可见性（修改立即刷新主内存）和禁止指令重排序。不保证原子性。
> **项目关联：** 异步任务状态更新需要保证可见性。

### Q: 线程池的核心参数？
> **答：** corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory、rejectedExecutionHandler。
> **项目关联：** AsyncTaskServiceImpl 使用 `Executors.newFixedThreadPool(4)` 处理文档。

### Q: CompletableFuture 和 Future 的区别？
> **答：** Future.get()会阻塞，CompletableFuture支持链式回调（thenApply/thenAccept）、组合（thenCombine）、异常处理（exceptionally）。
> **项目关联：** 文档异步处理使用 `CompletableFuture.runAsync()`。

### Q: 接口和抽象类的区别？
> **答：** 接口：多实现、只能有抽象方法（Java8后可有default方法）、没有状态。抽象类：单继承、可以有具体方法和成员变量。
> **项目关联：** AiTool 是接口（工具行为契约），不是抽象类（因为工具之间没有共性状态）。

### Q: Java 泛型的类型擦除？
> **答：** 编译后泛型信息被擦除，`List<String>` 和 `List<Integer>` 运行时都是 `List`。通过桥方法保证多态。

### Q: Java 异常体系？
> **答：** Throwable → Error（不可恢复）+ Exception → 受检异常（必须处理）+ RuntimeException（非受检）。
> **项目关联：** AiException 继承 RuntimeException，配合全局异常处理器统一拦截。

---

## 四、Spring Boot 相关问题

### Q: @SpringBootApplication 的组成？
> **答：** = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
> - @EnableAutoConfiguration 通过 spring.factories 加载自动配置类
> - @ComponentScan 扫描 @Component 注解的类

### Q: Spring Boot 自动配置原理？
> **答：** `@EnableAutoConfiguration` → `SpringFactoriesLoader` 加载 `META-INF/spring.factories` → 每个配置类用 `@ConditionalOnClass`/`@ConditionalOnMissingBean` 判断是否生效。
> **项目关联：** Druid 数据源、Redis、MyBatis 都是自动配置生效的。

### Q: Spring IoC 和 DI？
> **答：** IoC：对象创建权交给容器。DI：容器自动装配依赖。IoC是思想，DI是实现方式。
> **项目关联：** LlmServiceImpl 通过构造器注入 AiConfig。

### Q: @Transactional 的注意事项？
> **答：** 1) 同类方法内部调用失效（未经代理） 2) 方法非public 3) 异常被catch 4) 默认只回滚RuntimeException。
> **项目关联：** KbServiceImpl 的 processDocument 使用 @Transactional，如果切片保存失败会回滚文档状态。

### Q: Spring AOP 的实现方式？
> **答：** JDK 动态代理（接口）或 CGLIB（类）。默认：有接口用JDK代理，无接口用CGLIB。
> **项目关联：** 若依的 @Log 操作日志注解通过 AOP 实现。

### Q: Bean 的作用域？
> **答：** singleton（默认）、prototype、request、session、application。
> **项目关联：** 所有 Service 都是 singleton，ToolRegistry 也是 singleton。

### Q: 如何自定义 Starter？
> **答：** 1) 创建配置类 2) 创建 spring.factories 3) 打包为 jar。其他项目引入依赖即可自动配置。
> **项目关联：** ruoyi-ai 模块本质上就是一个 AI 功能的 Starter。

---

## 五、Redis 设计问题

### Q: 为什么用 Redis 存会话而不是 MySQL？
> **答：** 1) 性能：Redis 内存读写（微秒级）vs MySQL 磁盘IO（毫秒级）
> 2) TTL：Redis 原生支持过期，MySQL 需要定时清理
> 3) 数据结构：Redis List 天然适合时间序列数据
> **项目关联：** ConversationCacheServiceImpl 使用 Redis List 存储对话历史

### Q: Redis 的 Key 设计规范？
> **答：** `业务:对象:ID:属性`，如 `ai:chat:{userId}:{sessionId}`
> **项目关联：** `ai:chat:1001:abc123` 表示用户1001的abc123会话

### Q: 缓存穿透/击穿/雪崩？
> **答：** 穿透：查询不存在的数据→布隆过滤器。击穿：热点key过期→互斥锁。雪崩：大量key同时过期→随机TTL。

### Q: Redis 持久化方式？
> **答：** RDB（定时快照）+ AOF（追加写命令）。生产建议两者都开。

---

## 六、MySQL 设计问题

### Q: 为什么 kb_chunk 表不冗余文档标题？
> **答：** 遵循第三范式（3NF），通过 doc_id 外关联系联查。冗余会导致更新异常。

### Q: ai_audit_log 表为什么按 create_time 分区？
> **答：** 审计日志按时间增长，查询通常按时间范围。分区可以快速裁剪旧数据，提升查询性能。

### Q: 索引设计原则？
> **答：** 1) 最左前缀原则 2) 区分度高的列放前面 3) 避免过度索引 4) 覆盖索引减少回表。
> **项目关联：** ai_audit_log 建了 user_id、operation_type、create_time 三个索引。

### Q: 事务隔离级别？
> **答：** READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ(默认) → SERIALIZABLE
> **项目关联：** 文档处理使用 REPEATABLE READ，保证切片保存和文档状态更新的一致性。

---

## 七、Agent 相关问题

### Q: 什么是 Agent？和普通 LLM 对话的区别？
> **答：** Agent = LLM + Memory + Tools。普通 LLM 只能根据训练数据回答，Agent 能使用工具获取实时信息、执行操作。
> **项目中的体现：** Agent 可以查员工、查部门、搜知识库——这些 LLM 自身做不到。

### Q: Function Calling 的原理？
> **答：**
> 1. 调用 LLM 时传入 tools 参数（JSON Schema 定义）
> 2. LLM 分析意图，决定是否需要工具
> 3. 需要则返回 tool_calls（函数名+参数）
> 4. 开发者执行函数，结果通过 tool role 传回
> 5. LLM 整合结果生成最终回答

### Q: 为什么用 Agent 而不是直接写接口？
> **答：**
> 1. 灵活性：用户说自然语言，Agent 自动选择工具
> 2. 组合能力：一个问题可能需要多个工具
> 3. 推理能力：Agent 能根据上下文决定下一步
> 4. 可扩展：新增工具无需修改主流程

### Q: 如何防止 Agent 无限循环？
> **答：** 设置 MAX_TOOL_ROUNDS = 5，超过后强制返回。

### Q: 工具执行失败怎么办？
> **答：** 返回错误信息给 LLM（而非抛异常），LLM 可以尝试其他工具或告知用户。同时记录日志。

### Q: @AgentTool 注解的作用？
> **答：** 实现工具自动注册，符合开闭原则。新增工具只需：1. 实现 AiTool 接口 2. 加 @AgentTool 注解。无需修改 ToolRegistry。

---

## 八、RAG 相关问题

### Q: RAG 和微调的区别？
> **答：** RAG：成本低、数据实时、可追溯。微调：需要GPU、数据需标注、适合风格迁移。
> **项目选择 RAG 的原因：** 企业知识库更新频繁，RAG 更灵活。

### Q: 什么是 Embedding？
> **答：** 将文本映射为高维向量。语义相近的文本，向量距离更近。解决关键词匹配无法理解语义的问题。

### Q: 为什么不用 MySQL 做向量检索？
> **答：** MySQL 擅长结构化查询，不支持高效的高维向量相似度计算。向量数据库（Qdrant/Milvus）专门优化了 ANN 检索。

### Q: TopK 怎么选？
> **答：** 通常 3~5。K太小遗漏信息，K太大引入噪声。本项目默认 K=3，可通过 RagConfig 配置。

### Q: 如何解决 LLM 幻觉？
> **答：**
> 1. RAG 提供真实文档作为上下文
> 2. Prompt 明确要求"仅根据文档回答"
> 3. 低 temperature（0.3）减少随机性
> 4. 返回引用来源，用户可验证

### Q: 切片策略有哪些？
> **答：** 按段落（保持语义）、固定大小（均匀）、滑动窗口（有重叠）、按句子。本项目用"按段落+最大500字+50字重叠"。

### Q: 余弦相似度和欧氏距离？
> **答：** 余弦衡量方向相似性（-1到1），对长度不敏感。欧氏衡量绝对距离。文本相似度一般用余弦。

---

## 九、为什么使用若依二次开发

### Q: 为什么不从零开发？
> **答：**
> 1. 若依是国内最成熟的企业后台框架，RBAC 经过大量验证
> 2. 不重复造轮子，聚焦 AI 差异化能力
> 3. 若依代码规范，面试官容易理解
> 4. 体现"站在巨人肩膀上"的工程思维

### Q: 二次开发改了什么？
> **答：**
> - **不改的：** 用户/角色/菜单/认证/日志（若依基础能力）
> - **新增的：** ruoyi-ai 模块（~30个Java文件，~3000行代码）
> - **修改的：** pom.xml（添加AI模块依赖）、application.yml（添加AI配置）

### Q: 如何降低"二次开发"的感觉？
> **答：** 自研模块有完整的分层架构（Controller→Service接口→Service实现→Mapper）、自定义异常体系、异步任务、Redis缓存、审计日志，这些都不是简单调用若依API，而是独立的工程设计。

---

## 十、项目不足和优化方向

### 当前不足

| 不足 | 说明 |
|------|------|
| 向量检索 | 使用内存向量，数据量有限，生产环境应切换 Qdrant |
| LLM 依赖 | 需要外部 API（DeepSeek），离线场景可用 Ollama |
| 测试覆盖 | 暂无单元测试和集成测试 |
| 流式输出 | 当前同步返回，未实现 SSE 流式输出 |
| 文档格式 | 暂不支持 Excel、PPT 等格式 |

### 优化方向

| 方向 | 方案 |
|------|------|
| 向量数据库 | 接入 Qdrant/Milvus，支持百万级向量检索 |
| Reranker | 对检索结果重排序，提升准确率 |
| 流式输出 | 使用 SSE 实现逐字输出，改善用户体验 |
| 多模态 | 支持图片、表格的理解和检索 |
| 权限控制 | 知识库文档按部门权限隔离 |
| 监控面板 | Prometheus + Grafana 监控 AI 调用指标 |
