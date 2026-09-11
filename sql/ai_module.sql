-- =============================================
-- 企业智能知识管理 - AI模块数据库表
-- =============================================

-- 知识库文档表
DROP TABLE IF EXISTS kb_document;
CREATE TABLE kb_document (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    title       VARCHAR(255) NOT NULL COMMENT '文档标题',
    file_path   VARCHAR(500) DEFAULT NULL COMMENT '文件存储路径',
    file_type   VARCHAR(20)  DEFAULT NULL COMMENT '文件类型: pdf/docx/md/txt',
    file_size   BIGINT       DEFAULT 0 COMMENT '文件大小(字节)',
    content     LONGTEXT     DEFAULT NULL COMMENT '解析后的文本内容',
    chunk_count INT          DEFAULT 0 COMMENT '切片数量',
    status      TINYINT      DEFAULT 0 COMMENT '状态: 0-待处理 1-已解析 2-已向量化 3-失败',
    create_by   VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    update_time DATETIME     DEFAULT NULL COMMENT '更新时间',
    remark      VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_create_by (create_by)
) ENGINE=InnoDB COMMENT='知识库文档表';

-- 文档切片表
DROP TABLE IF EXISTS kb_chunk;
CREATE TABLE kb_chunk (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '切片ID',
    doc_id      BIGINT   NOT NULL COMMENT '文档ID',
    chunk_index INT      NOT NULL COMMENT '切片序号(从0开始)',
    content     TEXT     NOT NULL COMMENT '切片文本内容',
    token_count INT      DEFAULT 0 COMMENT '估算Token数',
    create_time DATETIME DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_doc_id (doc_id)
) ENGINE=InnoDB COMMENT='文档切片表';

-- AI对话历史表
DROP TABLE IF EXISTS ai_conversation;
CREATE TABLE ai_conversation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    session_id  VARCHAR(64)  NOT NULL COMMENT '会话ID',
    role        VARCHAR(20)  NOT NULL COMMENT '角色: user/assistant/system/tool',
    content     TEXT         DEFAULT NULL COMMENT '消息内容',
    msg_type    VARCHAR(20)  DEFAULT 'chat' COMMENT '消息类型: chat/rag/agent',
    tool_name   VARCHAR(100) DEFAULT NULL COMMENT '工具名称(agent调用时)',
    tool_args   TEXT         DEFAULT NULL COMMENT '工具参数(JSON)',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_user_session (user_id, session_id)
) ENGINE=InnoDB COMMENT='AI对话历史表';

-- Agent工具调用日志表
DROP TABLE IF EXISTS ai_tool_log;
CREATE TABLE ai_tool_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id     BIGINT       DEFAULT NULL COMMENT '用户ID',
    session_id  VARCHAR(64)  DEFAULT NULL COMMENT '会话ID',
    tool_name   VARCHAR(100) NOT NULL COMMENT '工具名称',
    input_args  TEXT         DEFAULT NULL COMMENT '输入参数(JSON)',
    output      TEXT         DEFAULT NULL COMMENT '输出结果',
    status      TINYINT      DEFAULT 1 COMMENT '状态: 1-成功 0-失败',
    duration    INT          DEFAULT 0 COMMENT '执行耗时(ms)',
    create_time DATETIME     DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_tool_name (tool_name)
) ENGINE=InnoDB COMMENT='Agent工具调用日志表';

-- 插入知识库菜单
INSERT INTO sys_menu VALUES (2000, '知识管理', 0, 3, 'knowledge', NULL, NULL, '1', '0', '', 'documentation', 'admin', sysdate(), '', NULL, '知识管理目录');
INSERT INTO sys_menu VALUES (2001, '文档管理', 2000, 1, 'document', 'ai/kb/document', NULL, '1', '0', 'ai:document:list', 'form', 'admin', sysdate(), '', NULL, '文档管理菜单');
INSERT INTO sys_menu VALUES (2002, 'AI助手', 0, 4, 'ai', NULL, NULL, '1', '0', '', 'education', 'admin', sysdate(), '', NULL, 'AI助手目录');
INSERT INTO sys_menu VALUES (2003, '智能问答', 2002, 1, 'chat', 'ai/chat', NULL, '1', '0', 'ai:chat:list', 'message', 'admin', sysdate(), '', NULL, '智能问答菜单');
