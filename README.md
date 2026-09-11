# 企业智能知识管理与Agent辅助办公平台

> 基于 RuoYi-Vue 二次开发，集成 RAG 知识问答与 Agent 智能助手的企业级信息化平台

## 一、项目背景

企业内部数字化办公场景中，员工面临以下痛点：

- 制度文档查询困难，散落在不同系统
- 项目资料分散，信息检索效率低
- 重复咨询成本高，新员工培训周期长

本平台在成熟的企业后台管理系统基础上，集成 AI 能力，实现：

```
用户登录 → 权限管理 → 知识库管理 → 文档解析 → 知识检索 → Agent智能问答 → 业务工具调用 → 结果返回
```

## 二、系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 (Vue 2 + Element UI)                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
│  │ 系统管理  │ │ 知识库    │ │ AI问答    │ │ Agent助手     │  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────────┘  │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP/REST
┌─────────────────────────┴───────────────────────────────────┐
│                  Spring Boot Application                     │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  Controller Layer                     │    │
│  │   SysController │ KbController │ AiController        │    │
│  └───────────────────────┬─────────────────────────────┘    │
│  ┌───────────────────────┴─────────────────────────────┐    │
│  │                  Service Layer (接口 + 实现)          │    │
│  │  ┌──────────┐ ┌──────────┐ ┌────────────────────┐  │    │
│  │  │ 系统服务  │ │ 知识库    │ │ AI Service         │  │    │
│  │  │(若依原生) │ │ KbService│ │ ┌────────────────┐ │  │    │
│  │  └──────────┘ └──────────┘ │ │RagService      │ │  │    │
│  │                             │ ├────────────────┤ │  │    │
│  │                             │ │AgentService    │ │  │    │
│  │                             │ ├────────────────┤ │  │    │
│  │                             │ │LlmService      │ │  │    │
│  │                             │ ├────────────────┤ │  │    │
│  │                             │ │EmbeddingService│ │  │    │
│  │                             │ └────────────────┘ │  │    │
│  │                             └────────────────────┘  │    │
│  └───────────────────────┬─────────────────────────────┘    │
│  ┌───────────────────────┴─────────────────────────────┐    │
│  │              Tool Layer (Agent工具)                   │    │
│  │  EmployeeTool │ DepartmentTool │ KnowledgeTool       │    │
│  └─────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Data Access Layer                       │    │
│  │  ┌──────┐ ┌───────┐ ┌──────────┐ ┌──────────────┐ │    │
│  │  │MySQL │ │Redis  │ │DeepSeek  │ │VectorStore   │ │    │
│  │  └──────┘ └───────┘ └──────────┘ └──────────────┘ │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## 三、技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 4.1 + Spring Security | 若依基础框架 |
| ORM | MyBatis + PageHelper | 数据持久化 |
| 数据库 | MySQL 8.4 | 业务数据 + 对话历史 |
| 缓存 | Redis | 会话缓存、Token管理 |
| HTTP客户端 | OkHttp 4.12 | 调用LLM API |
| 文档解析 | Apache Tika 2.9 | PDF/Word/MD解析 |
| AI模型 | DeepSeek API / Ollama | 聊天 + Embedding |
| 向量检索 | 内存向量索引 + 余弦相似度 | 可切换Qdrant |
| 前端 | Vue 2 + Element UI | 若依前端框架 |
| 部署 | Docker + Nginx | 容器化部署 |

## 四、核心模块

### 4.1 用户权限管理（若依基础）

- RBAC 权限模型：用户 → 角色 → 权限
- JWT Token 认证
- 数据权限（部门级数据隔离）
- 操作日志审计

### 4.2 知识库管理

**文档处理链路：**
```
文件上传(PDF/Word/MD) → Apache Tika解析 → 文本切片 → Embedding向量化 → 向量存储
```

**切片策略（ChunkStrategy）：**
- PARAGRAPH：按段落切分（默认，保持语义完整性）
- FIXED_SIZE：固定大小切分
- SENTENCE：按句子切分

**参数：** 最大500字/片，50字重叠窗口

### 4.3 RAG 知识问答

**RAG 流程：**
```
用户提问 → Query Embedding → 向量召回 TopK → Prompt构造(上下文+问题) → LLM生成 → 答案+引用
```

**关键设计：**
- TopK = 3（可配置）
- 相似度阈值过滤（score < 0.3 的结果不返回）
- 温度控制 = 0.3（减少幻觉）
- 答案附带引用来源，用户可验证

### 4.4 Agent 智能助手

**Agent 架构：**
```
Agent = LLM + Memory + Tools
```

**Function Calling 流程：**
```
用户输入 → 意图识别(LLM) → Tool选择 → 执行工具 → 结果返回LLM → 生成答案
```

**已实现工具：**

| 工具 | 功能 | 数据来源 |
|------|------|----------|
| query_employee | 查询员工信息 | 若依用户服务 |
| query_department | 查询部门信息 | 若依部门服务 |
| search_knowledge | 知识库语义检索 | RAG向量检索 |

**工具扩展机制：** `@AgentTool` 注解 + `AiTool` 接口，新增工具无需修改已有代码。

### 4.5 会话管理

- **Redis 短期缓存**：最近20轮对话，TTL 2小时
- **MySQL 长期存储**：全量对话历史，永久保存
- 读取优先级：Redis → MySQL（缓存未命中时自动回填）

### 4.6 操作日志审计

- **AI 调用日志**（ai_audit_log）：记录每次 AI 操作的用户、问题、工具、响应时间、状态
- **工具调用日志**（ai_tool_log）：记录 Agent 工具的输入输出和执行耗时
- **异步任务日志**（ai_async_task）：跟踪文档处理任务的状态流转

## 五、数据库设计

### 核心表（AI模块新增6张）

| 表名 | 说明 |
|------|------|
| kb_document | 知识库文档（标题/路径/类型/状态） |
| kb_chunk | 文档切片（内容/序号/Token数） |
| ai_conversation | 对话历史（用户/会话/角色/内容） |
| ai_tool_log | 工具调用日志 |
| ai_audit_log | AI调用审计日志 |
| ai_async_task | 异步任务状态 |

### 若依基础表（继承）

sys_user, sys_role, sys_menu, sys_dept, sys_oper_log 等

## 六、模块结构

```
ruoyi-ai/src/main/java/com/ruoyi/ai/
├── config/
│   ├── AiConfig.java            # AI全局配置
│   ├── RagConfig.java           # RAG参数配置
│   └── ChunkStrategy.java       # 切片策略枚举
├── controller/
│   ├── AiController.java        # AI问答接口（chat/rag/agent）
│   └── KbController.java        # 知识库管理接口
├── service/                     # 接口层（面向接口编程）
│   ├── LlmService.java
│   ├── EmbeddingService.java
│   ├── VectorStoreService.java
│   ├── KbService.java
│   ├── RagService.java
│   ├── AgentService.java
│   ├── ConversationCacheService.java
│   └── impl/                    # 实现层
│       ├── LlmServiceImpl.java
│       ├── EmbeddingServiceImpl.java
│       ├── VectorStoreServiceImpl.java
│       ├── KbServiceImpl.java
│       ├── RagServiceImpl.java
│       ├── AgentServiceImpl.java
│       ├── ConversationCacheServiceImpl.java
│       └── AsyncTaskServiceImpl.java
├── tool/                        # Agent工具层
│   ├── AiTool.java              # 工具接口
│   ├── AgentTool.java           # 工具注解
│   ├── ToolRegistry.java        # 工具注册中心
│   ├── EmployeeTool.java        # 员工查询工具
│   ├── DepartmentTool.java      # 部门查询工具
│   └── KnowledgeTool.java       # 知识检索工具
├── entity/                      # 数据实体
├── mapper/                      # MyBatis接口
├── domain/
│   ├── dto/                     # 请求DTO
│   ├── vo/                      # 响应VO
│   └── query/                   # 查询条件
└── exception/                   # 异常体系
    ├── AiException.java
    ├── LlmCallException.java
    ├── EmbeddingException.java
    ├── ToolExecutionException.java
    ├── DocumentProcessException.java
    └── AiExceptionHandler.java  # 全局异常处理
```

## 七、快速启动

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 16+（前端）

### 后端启动

```bash
# 1. 创建数据库
mysql -u root -e "CREATE DATABASE ruoyi DEFAULT CHARSET utf8mb4;"

# 2. 导入SQL
mysql -u root ruoyi < sql/ry_20260320.sql
mysql -u root ruoyi < sql/quartz.sql
mysql -u root ruoyi < sql/ai_module.sql

# 3. 修改配置（application-druid.yml 中数据库连接信息）

# 4. 配置AI（application.yml 中 ai.* 配置）
#    - 使用 DeepSeek: 设置 ai.api-key
#    - 使用 Ollama: 设置 ai.provider=ollama

# 5. 编译运行
mvn clean install -DskipTests
mvn spring-boot:run -pl ruoyi-admin
```

### 前端启动

```bash
cd ruoyi-ui
npm install
npm run dev
```

## 八、面试技术亮点

1. **RAG 检索增强生成**：直接用 OkHttp 调用 Embedding/Chat API，不依赖框架，体现对底层协议的理解
2. **Agent Function Calling**：实现 OpenAI 兼容的工具调用协议，支持多轮工具调用循环
3. **工具扩展机制**：`@AgentTool` 注解 + `AiTool` 接口，符合开闭原则
4. **面向接口编程**：所有 Service 拆分接口/实现，便于 Mock 测试和实现替换
5. **Redis + MySQL 双层缓存**：会话历史读写优化，Redis 做短期缓存，MySQL 做持久化
6. **异步任务处理**：文档上传后异步处理，前端轮询状态，用户体验好
7. **完整异常体系**：自定义异常层级 + 全局异常处理器，统一错误响应格式
8. **审计日志**：所有 AI 调用留痕，满足央国企合规要求

## 九、基于若依二次开发说明

### 继承的能力（不修改）

- 用户/角色/菜单/部门管理
- JWT 认证 + Spring Security
- 操作日志（@Log 注解 AOP）
- 文件上传、分页、全局异常

### 新增的模块

- `ruoyi-ai` 模块：~30 个 Java 文件，~3000 行核心代码
- 6 张数据库表
- 知识库管理 + RAG 问答 + Agent 助手
- 完整的工程化设计（接口分层、异常体系、审计日志、异步任务）

## 十、License

基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 二次开发，遵循原项目 MIT License。
