# 面试准备材料

> 基于项目真实代码和 E2E 测试报告生成，不虚构不存在的能力

---

## 一、项目三分钟介绍

### 央国企/银行版

> 我的项目是"企业智能知识管理与Agent辅助办公平台"，基于若依企业后台管理系统进行二次开发，面向企业内部数字化办公场景。
>
> **背景：** 企业内部有大量制度文档、项目资料，员工查找信息效率低，新员工培训周期长，重复咨询成本高。
>
> **架构：** 系统分为两层。基础层是若依提供的用户权限管理、RBAC模型、JWT认证、操作日志。AI层是我自主开发的`ruoyi-ai`模块，包含知识库管理、RAG智能问答、Agent智能助手三个核心能力。
>
> **个人负责：** 我独立完成了AI模块的全部设计和开发，包括后端架构设计、数据库设计、AI链路实现、单元测试和端到端验证。
>
> **关键技术：** 1) 直接用OkHttp调用大模型API，不依赖AI框架，体现对底层协议的理解；2) 基于OpenAI Function Calling协议实现Agent工具调用闭环；3) 设计了异步任务模型解耦HTTP请求和文档处理；4) 通过注解扫描实现工具插件化扩展。
>
> **难点：** RAG的切片策略设计——需要在语义完整性和检索精度之间平衡，最终采用"按段落+最大长度+重叠窗口"的混合策略。Agent的多轮工具调用需要正确维护tool_calls和tool_call_id，保证协议兼容性。
>
> **优化方向：** 向量存储当前为内存实现，生产环境可替换为Qdrant；可接入Reranker提升检索精度。

### 互联网版

> 企业智能知识管理平台，若依二开 + RAG + Agent。核心是`ruoyi-ai`模块：文档上传后异步解析向量化，用户通过自然语言提问，系统先在知识库检索相关文档再让LLM回答（RAG）。Agent模式下LLM可以自动调用业务工具查询员工、部门等信息，基于Function Calling协议实现工具调用闭环。工具通过注解自动注册，新增工具零修改。

---

## 二、项目架构讲解

### 2.1 Spring Boot模块

```
ruoyi-admin       → 启动入口、配置文件
ruoyi-common      → 通用工具、基础实体、注解
ruoyi-framework   → Security、AOP、拦截器
ruoyi-system      → 用户/角色/菜单/部门 CRUD
ruoyi-quartz      → 定时任务
ruoyi-generator   → 代码生成
ruoyi-ai          → ★ AI模块（自主开发）
```

### 2.2 AI模块结构

```
ruoyi-ai/
├── config/          AiConfig, RagConfig, ChunkStrategy
├── controller/      AiController, KbController
├── service/         9个接口 + 8个实现
├── tool/            @AgentTool注解 + AiTool接口 + 3个工具
├── entity/          6个实体
├── mapper/          6个Mapper + 6个XML
├── domain/          DTO/VO/Query
├── exception/       5个自定义异常 + 全局处理器
└── test/            7个测试类
```

### 2.3 RAG流程

```
用户提问
  ↓
Embedding（nomic-embed-text → 768维向量）
  ↓
Vector Search（内存向量 + 余弦相似度 → TopK=3）
  ↓
Similarity Filter（RagConfig.similarityThreshold=0.3）
  ↓
Prompt构造（System提示词 + 检索到的文档片段 + 用户问题）
  ↓
LLM生成（qwen2.5:7b）
  ↓
返回答案 + 引用来源
```

### 2.4 Agent流程

```
用户消息 + 工具定义 → LLM
  ↓ LLM返回 tool_calls
执行工具 → 结果通过 tool role 传回 LLM
  ↓ LLM整合结果
生成最终答案（最多循环5轮）
```

### 2.5 数据库设计

| 表 | 说明 |
|----|------|
| kb_document | 知识库文档（标题/路径/类型/状态） |
| kb_chunk | 文档切片（内容/序号/Token数） |
| ai_conversation | 对话历史（用户/会话/角色/内容） |
| ai_audit_log | AI调用审计日志 |
| ai_tool_log | 工具调用日志 |
| ai_async_task | 异步任务状态 |

### 2.6 Redis设计

```
Key: ai:chat:{userId}:{sessionId}
Value: List<AiConversation>
TTL: 2小时
策略：写入时双写Redis+MySQL，读取时Redis优先→miss→MySQL→回填
```

### 2.7 异步任务设计

```
上传文件 → 保存文档记录 → 提交异步任务 → 立即返回taskId
后台：Tika解析 → Chunk → Embedding → 向量存储
状态机：WAITING → RUNNING → SUCCESS / FAILED
线程池：ThreadPoolExecutor(core=2, max=4, queue=20, CallerRunsPolicy)
```

---

## 三、Java后端深挖问题

### Q1: Spring Bean的生命周期？
> 实例化 → 属性注入 → Aware接口回调 → BeanPostProcessor前置 → InitializingBean/init-method → BeanPostProcessor后置 → 使用 → DisposableBean/destroy-method。
> **项目体现：** VectorStoreServiceImpl的`@PostConstruct`在初始化阶段从MySQL加载切片元数据。

### Q2: IoC和AOP的区别？
> IoC是控制反转（对象创建权交给容器），AOP是面向切面编程（在不修改原代码的情况下增强方法）。IoC通过DI实现，AOP通过动态代理实现。
> **项目体现：** 所有Service通过构造器注入依赖（IoC）；若依的`@Log`注解通过AOP记录操作日志。

### Q3: MyBatis的执行流程？
> 1) 加载mybatis-config.xml和Mapper.xml 2) 创建SqlSessionFactory 3) 获取SqlSession 4) 通过Mapper代理执行SQL 5) 结果映射返回。
> **项目体现：** `mapUnderscoreToCamelCase`配置启用后，`file_path`自动映射到`filePath`字段。

### Q4: @Transactional的注意事项？
> 1) 同类方法内部调用绕过代理 2) 方法非public 3) 异常被catch 4) 默认只回滚RuntimeException。
> **项目体现：** KbServiceImpl的`saveDocumentRecord`、`saveChunks`、`updateDocStatus`独立标注@Transactional，避免在长流程中持有事务。

### Q5: 线程池参数如何设计？
> corePoolSize=2（IO密集型，核心数可少）、maxPoolSize=4、queue=20（LinkedBlockingQueue）、keepAliveTime=60s、rejectedExecutionPolicy=CallerRunsPolicy（队列满时由提交线程执行，保证任务不丢失）。
> **项目体现：** AsyncTaskServiceImpl用于文档异步处理。

### Q6: Redis缓存设计？
> Key设计：`ai:chat:{userId}:{sessionId}`，Value：对话历史列表，TTL：2小时。写入双写Redis+MySQL，读取Redis优先→miss→MySQL→回填。
> **项目体现：** ConversationCacheServiceImpl。

### Q7: MySQL索引设计？
> kb_document表：idx_status（按状态查询）、idx_create_by（按创建者查询）。ai_audit_log表：idx_user_id、idx_operation、idx_create_time。
> **项目体现：** 文档列表查询和审计日志查询。

### Q8: JWT认证流程？
> 登录 → 生成JWT Token → 前端存储 → 每次请求放在Authorization header → Security过滤器解析Token → 验证有效期 → 存入SecurityContext。
> **项目体现：** 若依的登录认证，Token有效期30分钟。

### Q9: 异常处理设计？
> 自定义异常体系：AiException基类 → LlmCallException、EmbeddingException、ToolExecutionException、DocumentProcessException。全局异常处理器AiExceptionHandler统一拦截，返回`{code, msg}`格式。
> **项目体现：** Controller不自行catch，统一交给AiExceptionHandler。

---

## 四、RAG专项问题

### Q1: 什么是Embedding？
> 将文本映射为高维向量的过程。语义相近的文本，向量距离更近。解决关键词匹配无法理解语义的问题（如"请假"和"休假申请"）。
> **项目体现：** 使用nomic-embed-text模型，输出768维向量。

### Q2: Chunk切片策略有哪些？
> 按段落（保持语义完整）、固定大小（均匀但可能截断）、滑动窗口（有重叠）。本项目采用"按段落+最大500字+50字重叠"的混合策略。

### Q3: 什么是余弦相似度？
> cosine(A,B) = (A·B) / (|A| × |B|)，值域[-1,1]，越接近1越相似。文本相似度一般用余弦而非欧氏距离，因为对向量长度不敏感。

### Q4: TopK怎么选？
> 通常3~5。K太小遗漏信息，K太大引入噪声。本项目默认K=3，通过RagConfig外部化配置。

### Q5: Prompt如何构造？
> System提示词（角色定义+行为约束）+ 检索到的文档片段（上下文）+ 用户问题。Prompt中明确要求"仅根据文档回答，如无相关信息请说明"。

### Q6: 如何解决LLM幻觉？
> 1) RAG提供真实文档约束回答 2) Prompt明确要求基于文档 3) 低temperature(0.3)减少随机性 4) 返回引用来源可验证。

### Q7: 为什么不用MySQL直接查？
> MySQL擅长结构化查询，不支持高效的高维向量相似度计算。向量数据库专门优化了ANN检索。

---

## 五、Agent专项问题

### Q1: Function Calling的原理？
> 1) 调用LLM时传入tools参数（JSON Schema定义工具） 2) LLM分析意图决定是否需要工具 3) 需要则返回tool_calls 4) 开发者执行工具 5) 结果通过tool role传回 6) LLM整合结果生成最终回答。

### Q2: tool schema如何设计？
> 遵循OpenAI Function Calling规范：`{type:"function", function:{name, description, parameters:{type:"object", properties:{...}, required:[...]}}}`。description对LLM理解工具用途至关重要。

### Q3: Tool自动注册如何实现？
> `@AgentTool`注解标记工具类，`AiTool`接口定义执行方法。ToolRegistry通过Spring注入`List<AiTool>`，`@PostConstruct`扫描注解构建Map和JSON Schema。新增工具只需实现接口+加注解，零修改。

### Q4: 多轮工具调用如何控制？
> 设置MAX_TOOL_ROUNDS=5防止无限循环。每轮检查LLM响应是否包含tool_calls，有则执行并继续，无则返回最终答案。

### Q5: 上下文如何管理？
> 使用ChatMessage DTO统一管理role/content/tool_calls/tool_call_id。从Redis/MySQL加载最近N轮历史作为上下文传给LLM。

### Q6: Agent和普通Chat的区别？
> 普通Chat：用户必须知道调用什么接口、传什么参数。Agent：用户说自然语言，LLM自动判断需要什么工具、怎么调用。Agent还能组合多个工具完成复杂任务。

---

## 六、项目追问模拟

### Q: 为什么不用Milvus/Qdrant？
> 当前是演示项目，数据量小（百级文档），内存向量+余弦相似度完全够用。通过VectorStoreService接口抽象，生产环境可无缝替换为Qdrant，只需新增一个实现类。体现了面向接口编程的设计思想。

### Q: 为什么不用Spring AI/LangChain4j？
> 1) Spring AI对Spring Boot 4.x兼容性不确定 2) 直接用OkHttp调用API能完全掌控请求响应细节 3) 面试时能讲清楚底层原理，比用框架更有说服力 4) 接口设计兼容，未来可无缝切换。

### Q: 为什么自己实现Agent而不是用现成框架？
> 1) 理解底层协议比使用框架更重要 2) Function Calling协议本身不复杂，自己实现可控性更强 3) 工具注册通过注解+接口实现，扩展性好 4) 面试时能讲清楚每一步的实现细节。

### Q: 如何保证工具调用安全？
> 1) RBAC权限控制（@PreAuthorize） 2) 敏感字段脱敏（手机号/邮箱） 3) 工具执行设超时 4) 最大调用轮次限制 5) 所有调用记录审计日志。

### Q: 如何扩展新工具？
> 两步：1) 实现AiTool接口 2) 加@AgentTool注解。无需修改ToolRegistry或其他已有代码。体现了开闭原则。

### Q: 如何优化性能？
> 1) 向量存储替换为Qdrant（ANN检索） 2) Embedding批量异步处理 3) 引入Reranker重排序 4) Redis缓存热点查询 5) 流式输出改善体验。

### Q: Spring Boot 4.0是什么版本？
> 这是若依v3.9.2框架自带的版本选择。若依团队在最新版本中升级到了Spring Boot 4.x以支持Jakarta EE。我在二次开发时保持了框架的版本一致性，避免引入兼容性问题。

---

## 七、简历版本

### 三行版
```
企业智能知识管理与Agent辅助办公平台 | 基于若依二次开发 | Spring Boot + MyBatis + MySQL + Redis + Ollama
• 设计并实现RAG知识问答链路：文档异步解析→文本切片→Embedding向量化→向量检索→LLM生成回答
• 实现Agent智能助手：基于OpenAI Function Calling协议，支持工具自动注册和多轮调用，查询员工/部门/知识库
```

### 五行版
```
企业智能知识管理与Agent辅助办公平台 | Spring Boot + MyBatis + MySQL + Redis + Ollama
• 基于若依企业后台管理系统二次开发，新增ruoyi-ai模块（50+Java文件，2000+行核心代码）
• 实现RAG知识问答：文档上传→Tika解析→文本切片→Embedding→向量检索→Prompt构造→LLM回答
• 实现Agent智能助手：基于Function Calling协议，@AgentTool注解自动注册，支持3个业务工具
• 设计异步任务模型：ThreadPoolExecutor解耦HTTP请求和文档处理，状态机管理WAITING→SUCCESS/FAILED
• Redis+MySQL双层会话缓存，完整审计日志体系，@PreAuthorize权限控制，字段脱敏
```

### 互联网版
```
企业智能知识管理平台 | Java后端 + AI应用
• 若依二开 + 自研AI模块，RAG知识问答 + Agent Function Calling
• 核心：文档异步解析向量化、余弦相似度检索、LLM生成、工具自动注册、多轮FC调用
• 技术：Spring Boot + MyBatis + MySQL + Redis + OkHttp + Ollama
```

### 央国企版
```
企业智能知识管理与Agent辅助办公平台 | 信息化系统开发
• 基于成熟企业后台框架二次开发，集成AI智能问答能力
• 实现企业知识库管理、RAG智能检索、Agent办公助手三大模块
• 技术栈：Spring Boot + MyBatis + MySQL + Redis，符合企业级技术选型标准
```
