# 面试准备材料

> 面向：银行科技岗、央国企信息科技岗
> 基于当前代码，不虚构不存在的能力

---

## 一、3分钟项目介绍

> 我的项目是"企业智能办公Agent平台"，基于若依企业后台管理系统二次开发。
>
> **背景：** 企业内部文档分散、查询效率低。我在若依的用户权限基础上，新增了知识库管理、RAG智能问答、Agent智能助手、Workflow任务编排、AI评估闭环五个核心模块。
>
> **架构：** 基础层是若依提供的RBAC权限、JWT认证、操作日志。AI层是我自主开发的ruoyi-ai模块，74个Java文件，约2900行代码。
>
> **关键技术：** 1) 直接用OkHttp调用大模型API，不依赖AI框架；2) 基于OpenAI Function Calling协议实现Agent工具调用，6个工具通过注解自动注册；3) Workflow引擎支持多节点任务编排；4) AI评估闭环形成反馈-BadCase-优化-提升循环。
>
> **难点：** Agent的多轮工具调用需要正确维护tool_calls和tool_call_id，保证协议兼容性。RAG的切片策略需要在语义完整性和检索精度之间平衡。

---

## 二、个人负责内容

1. 架构设计：AI模块整体架构，数据库设计（7张新表），接口设计
2. RAG实现：文档解析-切片-Embedding-向量检索-Prompt-LLM生成
3. Agent实现：Function Calling协议、Tool自动注册、多轮调用循环
4. Tool开发：6个业务工具（员工/部门/知识/项目/审批/报告）
5. Workflow引擎：节点接口、调度器、周报生成Workflow
6. AI评估：反馈系统、Bad Case管理
7. 工程质量：异常体系、异步任务、Redis缓存、审计日志

---

## 三、Java技术问题

**Q1: HashMap底层原理？**
数组+链表+红黑树。默认容量16，负载因子0.75。链表长度>=8且数组>=64时转红黑树。
项目体现：VectorStoreServiceImpl用ConcurrentHashMap存向量索引。

**Q2: ConcurrentHashMap和HashMap区别？**
ConcurrentHashMap线程安全，JDK8用CAS+synchronized锁单个Node。
项目体现：向量索引和切片缓存使用ConcurrentHashMap。

**Q3: 线程池参数如何设计？**
core=2, max=4, queue=20(LinkedBlockingQueue), keepAlive=60s, CallerRunsPolicy。
项目体现：AsyncTaskServiceImpl用于文档异步处理。

**Q4: 接口和抽象类区别？**
接口：多实现、只能有抽象方法。抽象类：单继承、可以有具体方法。
项目体现：AiTool是接口，WorkflowNode是接口。

**Q5: Java异常体系？**
Throwable-Error+Exception-RuntimeException(非受检)+受检异常。
项目体现：AiException继承RuntimeException，配合全局异常处理器。

---

## 四、Spring问题

**Q1: Spring Bean生命周期？**
实例化-属性注入-Aware回调-BeanPostProcessor前置-InitializingBean-BeanPostProcessor后置-使用-DisposableBean。
项目体现：VectorStoreServiceImpl的@PostConstruct从MySQL加载切片。

**Q2: IoC和AOP？**
IoC：对象创建权交给容器。AOP：面向切面编程，通过动态代理实现。
项目体现：所有Service通过构造器注入（IoC）。若依的@Log注解通过AOP记录日志。

**Q3: @Transactional注意事项？**
1) 同类调用绕过代理 2) 非public方法 3) 异常被catch 4) 默认只回滚RuntimeException。
项目体现：KbServiceImpl拆分小事务方法，避免在长流程中持有事务。

**Q4: Spring Boot自动配置原理？**
@EnableAutoConfiguration-SpringFactoriesLoader加载spring.factories-条件注解决定是否生效。
项目体现：Druid数据源、Redis、MyBatis自动配置。

**Q5: @PreAuthorize原理？**
基于Spring AOP，在方法执行前检查用户权限。需要@EnableMethodSecurity开启。
项目体现：AI接口配置了ai:chat:use、ai:kb:list等权限标识。

---

## 五、MySQL问题

**Q1: 索引数据结构？**
InnoDB用B+树。非叶子节点只存key，叶子节点存数据且用链表连接。3层可存千万级数据。
项目体现：ai_audit_log的idx_create_time加速时间范围查询。

**Q2: 事务隔离级别？**
READ UNCOMMITTED-READ COMMITTED-REPEATABLE READ(默认)-SERIALIZABLE。
项目体现：文档处理使用REPEATABLE READ。

**Q3: 慢SQL优化？**
EXPLAIN分析-添加索引-避免SELECT*-优化JOIN-分页优化。
项目体现：kb_document的idx_status索引加速按状态查询。

---

## 六、Redis问题

**Q1: 为什么用Redis存会话？**
1) 性能：内存读写微秒级 2) TTL原生支持过期 3) List适合时间序列。
项目体现：ConversationCacheServiceImpl。

**Q2: 缓存穿透/击穿/雪崩？**
穿透：查不存在的数据，用布隆过滤器。击穿：热点key过期，用互斥锁。雪崩：大量key同时过期，用随机TTL。

**Q3: Redis Key设计？**
ai:chat:{userId}:{sessionId}，TTL 2小时。
项目体现：写入双写Redis+MySQL，读取Redis优先，miss则MySQL，再回填Redis。

---

## 七、Agent问题

**Q1: Function Calling原理？**
1) 传入tools参数（JSON Schema） 2) LLM返回tool_calls 3) 执行工具 4) 结果通过tool role回传 5) LLM整合生成答案。

**Q2: 为什么用Agent而不是直接接口？**
1) 用户说自然语言，Agent自动选工具 2) 能组合多个工具 3) 能根据上下文推理。

**Q3: Tool自动注册如何实现？**
@AgentTool注解+AiTool接口。ToolRegistry注入List of AiTool，@PostConstruct扫描注解构建Map和Schema。新增工具零修改。

**Q4: 多轮工具调用如何控制？**
MAX_TOOL_ROUNDS=5。每轮检查tool_calls，有则执行并继续，无则返回最终答案。

**Q5: Agent和普通Chat区别？**
普通Chat：用户必须知道调用什么。Agent：用户说自然语言，LLM自动判断需要什么工具。

---

## 八、RAG问题

**Q1: RAG和微调区别？**
RAG：成本低、数据实时、可追溯。微调：需GPU、数据标注、适合风格迁移。

**Q2: Chunk切片策略？**
按段落+最大500字+50字重叠。保持语义完整性，重叠保证上下文连贯。

**Q3: 为什么不用MySQL做向量检索？**
MySQL不支持高效高维向量相似度计算。向量数据库专门优化ANN检索。

**Q4: 如何解决LLM幻觉？**
1) RAG提供真实文档约束 2) Prompt要求基于文档回答 3) 低temperature 4) 返回引用来源。

**Q5: TopK怎么选？**
通常3~5。K太小遗漏信息，K太大引入噪声。本项目K=3，通过RagConfig配置。

---

## 九、Workflow问题

**Q1: Workflow架构？**
WorkflowNode接口定义execute()方法。WorkflowExecutor负责节点调度和状态流转。每个节点执行后更新wf_task状态。

**Q2: 为什么不用现成工作流引擎？**
1) 企业场景简单，不需要复杂的BPMN 2) 轻量级实现可控性更强 3) 面试时能讲清楚每一步。

**Q3: 状态如何管理？**
wf_instance记录整体状态，wf_task记录每个节点状态。任一节点失败则整体FAILED。

**Q4: 如何扩展新Workflow？**
实现WorkflowNode接口，编排节点列表，调用WorkflowExecutor.executeWorkflow()。