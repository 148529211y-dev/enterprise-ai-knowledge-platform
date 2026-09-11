# 项目架构分析报告 — PROJECT_REVIEW.md

## 一、当前模块结构

```
RuoYi-Vue/
├── ruoyi-admin        (24 Java)  — 启动模块、Web入口
├── ruoyi-common       (109 Java) — 通用工具、基础Domain、注解、异常
├── ruoyi-framework    (45 Java)  — Security、AOP、拦截器、配置
├── ruoyi-system       (56 Java)  — 用户/角色/菜单/部门/日志 CRUD
├── ruoyi-quartz       (18 Java)  — 定时任务
├── ruoyi-generator    (13 Java)  — 代码生成
└── ruoyi-ai           (18 Java)  — ★ 自主开发：AI智能模块
```

**比例：若依原生 265 文件 vs 自主开发 18 文件 → 自研占比 6.3%**

---

## 二、已继承若依能力

| 能力 | 模块 | 说明 |
|------|------|------|
| 用户管理 | ruoyi-system | 用户CRUD、密码加密、状态管理 |
| 角色管理 | ruoyi-system | RBAC角色、数据权限 |
| 菜单管理 | ruoyi-system | 动态菜单、权限标识 |
| 部门管理 | ruoyi-system | 树形组织架构 |
| 登录认证 | ruoyi-framework | JWT Token + Spring Security |
| 操作日志 | ruoyi-system | @Log注解AOP记录 |
| 全局异常 | ruoyi-framework | GlobalExceptionHandler |
| 文件上传 | ruoyi-common | MultipartFileUtils |
| 分页 | ruoyi-common | PageHelper集成 |
| Redis | ruoyi-framework | RedisCache工具类 |

---

## 三、当前自主开发代码清单

| 文件 | 行数 | 职责 |
|------|------|------|
| AiConfig | 41 | AI配置属性 |
| AiController | 63 | AI问答接口（chat/rag/agent） |
| KbController | 60 | 知识库文档管理接口 |
| ChatRequest | 18 | 请求DTO |
| ChatResponse | 38 | 响应DTO |
| AiConversation | 40 | 对话历史实体 |
| KbChunk | 27 | 文档切片实体 |
| KbDocument | 49 | 知识库文档实体 |
| AiConversationMapper | 9 | 对话Mapper接口 |
| KbChunkMapper | 10 | 切片Mapper接口 |
| KbDocumentMapper | 10 | 文档Mapper接口 |
| LlmService | 140 | LLM API调用（OkHttp） |
| EmbeddingService | 105 | Embedding向量化 |
| VectorStoreService | 125 | 内存向量存储与检索 |
| KbService | 246 | 文档解析/切片/向量化 |
| RagService | 95 | RAG检索增强生成 |
| AgentService | 170 | Agent + Function Calling |
| ToolRegistry | 185 | 工具注册中心（3个Tool） |
| **合计** | **~1,500** | |

---

## 四、工程能力不足分析

### 4.1 分层架构问题

**现状：**
- Service 层直接用 `impl` 包，没有接口定义
- 违反"面向接口编程"原则
- 不利于单元测试 Mock

**应该：**
```
service/
├── LlmService.java           (接口)
├── impl/LlmServiceImpl.java  (实现)
```

### 4.2 缺失 DTO/VO 分层

**现状：**
- 只有 `ChatRequest`(DTO) 和 `ChatResponse`(DTO)
- Entity 直接暴露给 Controller

**应该：**
```
domain/
├── dto/       — 前端入参
├── vo/        — 返回前端（脱敏、格式化）
├── query/     — 查询条件对象
```

### 4.3 无参数校验

**现状：** Controller 层无任何 `@Valid`、`@NotNull` 注解

### 4.4 无自定义异常体系

**现状：** 所有异常用 `try-catch` + 字符串返回

**应该：**
- `AiException` 基类
- `LlmCallException`、`EmbeddingException`、`ToolExecutionException`
- 全局异常处理器统一拦截

### 4.5 无异步处理

**现状：** 文档上传后同步处理（解析+切片+Embedding），用户必须等待

**应该：** 上传后立即返回，后台异步处理，前端轮询状态

### 4.6 无 Redis 缓存设计

**现状：** 对话历史只存 MySQL，未用 Redis 做短期缓存

### 4.7 无 AI 专属操作日志

**现状：** 若依有通用操作日志，但缺少 AI 调用专属日志（Token数、响应时间、工具调用链）

### 4.8 工具注册硬编码

**现状：** `ToolRegistry` 用 switch-case 分发，新增工具需修改源码

**应该：** 注解扫描 + 接口抽象，自动注册工具

### 4.9 VectorStore 无接口抽象

**现状：** `VectorStoreService` 直接硬编码内存实现，无法切换 Qdrant

### 4.10 无任务状态管理

**现状：** 文档处理状态只有 `status` 字段，无独立任务表

---

## 五、改造方案

### 阶段2：Java工程能力增强

| 改造项 | 新增文件数 | 说明 |
|--------|-----------|------|
| Service 接口层 | +6 | 每个 Service 拆出接口 |
| DTO/VO/Query | +6 | 补充视图对象和查询对象 |
| 参数校验注解 | 修改现有 | Entity/DTO 加校验注解 |
| 自定义异常 | +4 | AiException 体系 |
| 全局异常处理 | +1 | AiExceptionHandler |
| 异步任务 | +3 | AsyncTask + 状态表 |
| Redis缓存 | +1 | ConversationCacheService |
| AI操作日志 | +2 | 实体 + Mapper |

预计新增：~23 个文件，~800 行代码

### 阶段3：Agent能力增强

| 改造项 | 新增文件数 | 说明 |
|--------|-----------|------|
| Tool接口抽象 | +2 | @AgentTool注解 + Tool接口 |
| ProjectTool | +1 | 查询项目状态工具 |
| 工具日志 | +1 | ToolExecutionLogService |
| 意图识别 | 修改AgentService | 增加意图分类步骤 |

预计新增：~4 个文件，~300 行代码

### 阶段4：RAG能力增强

| 改造项 | 新增文件数 | 说明 |
|--------|-----------|------|
| VectorStore接口 | +1 | 抽象接口 + 工厂 |
| Chunk策略枚举 | +1 | 策略模式 |
| RAG配置 | +1 | TopK、温度等可配 |

预计新增：~3 个文件，~200 行代码

### 阶段5-6：文档

- README.md 重写
- INTERVIEW_GUIDE.md 生成

---

## 六、改造后预期指标

| 指标 | 改造前 | 改造后 |
|------|--------|--------|
| 自研Java文件数 | 18 | ~45 |
| 自研代码行数 | ~1,500 | ~3,000 |
| 自研占比 | 6.3% | ~14% |
| Service接口覆盖 | 0% | 100% |
| 参数校验覆盖 | 0% | 80% |
| 异常体系 | 无 | 完整 |
| 异步能力 | 无 | 有 |
| Redis利用 | 无 | 会话缓存 |
| Tool扩展性 | switch-case | 注解自动注册 |
