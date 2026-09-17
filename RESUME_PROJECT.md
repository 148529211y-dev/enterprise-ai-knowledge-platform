# 简历项目描述 — RESUME_PROJECT.md

---

## 项目信息

**项目名称：** 企业智能办公Agent平台

**技术栈：** Spring Boot 4.0、Spring Security、MyBatis、MySQL 8.4、Redis、OkHttp、Apache Tika、Ollama

**AI技术：** RAG检索增强生成、OpenAI Function Calling、Embedding向量化、余弦相似度检索、Workflow任务编排

**项目规模：** 74个Java文件、约3400行核心代码、44张数据库表、6个Agent工具

---

## 三条项目描述

### 描述1（综合版）
基于若依企业后台管理系统二次开发，设计并实现企业智能办公Agent平台。新增ruoyi-ai模块，集成RAG知识问答、Agent智能助手、Workflow任务编排、AI评估闭环四大核心能力。实现文档异步解析、Embedding向量化、Function Calling工具调用、多节点Workflow编排等关键技术，支持6个业务工具通过注解自动注册。

### 描述2（Agent版）
基于OpenAI Function Calling协议设计Agent工具调用体系，实现员工查询、部门查询、知识检索、项目状态查询、审批记录查询、项目周报生成6个业务工具的自然语言调用。通过@AgentTool注解+AiTool接口实现工具插件化扩展，新增工具零修改已有代码。支持多轮工具调用循环，最多5轮，每轮正确维护tool_calls和tool_call_id协议字段。

### 描述3（RAG+Workflow版）
设计并实现企业知识库RAG问答链路：文档上传后通过Apache Tika异步解析，采用段落切片+最大500字+50字重叠策略，Embedding向量化后存入内存索引，用户提问时通过余弦相似度检索TopK文档片段构造Prompt，LLM基于真实文档生成回答。设计轻量级Workflow任务编排框架，支持多节点顺序执行和状态管理，实现项目数据查询和自动周报生成流程。

---

## 央国企版描述

基于成熟企业后台框架进行二次开发，新增智能办公模块，实现企业知识库管理、智能问答、Agent办公助手、任务编排、效果评估五大功能。技术栈采用Spring Boot + MyBatis + MySQL + Redis，符合企业级技术选型标准。通过RAG技术实现企业文档智能检索，通过Agent技术实现业务系统自然语言交互。

---

## 面试关键词

- 若依二次开发
- RBAC权限模型
- Spring Boot自动配置
- MyBatis Mapper扫描
- Redis会话缓存
- ThreadPoolExecutor异步任务
- RAG检索增强生成
- Function Calling协议
- Tool自动注册（注解+接口）
- Workflow任务编排
- AI评估闭环
- 统一异常处理
- 操作日志审计