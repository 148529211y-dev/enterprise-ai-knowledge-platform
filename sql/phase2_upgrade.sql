-- =============================================
-- 阶段1-3：新增业务表
-- =============================================

-- 项目信息表
DROP TABLE IF EXISTS biz_project;
CREATE TABLE biz_project (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    project_name  VARCHAR(200) NOT NULL COMMENT '项目名称',
    manager       VARCHAR(64)  DEFAULT NULL COMMENT '负责人',
    status        VARCHAR(20)  DEFAULT '进行中' COMMENT '状态: 进行中/已完成/已暂停/已取消',
    progress      INT          DEFAULT 0 COMMENT '进度百分比',
    risk          VARCHAR(20)  DEFAULT '低' COMMENT '风险等级: 低/中/高',
    description   VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
    create_time   DATETIME     DEFAULT NULL,
    update_time   DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='项目信息表';

-- 审批记录表
DROP TABLE IF EXISTS biz_approval;
CREATE TABLE biz_approval (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    applicant     VARCHAR(64)  NOT NULL COMMENT '申请人',
    type          VARCHAR(50)  NOT NULL COMMENT '审批类型: 报销/请假/采购/出差',
    title         VARCHAR(200) NOT NULL COMMENT '审批标题',
    amount        DECIMAL(12,2) DEFAULT NULL COMMENT '金额(报销/采购)',
    status        VARCHAR(20)  DEFAULT '待审批' COMMENT '状态: 待审批/已通过/已拒绝/已撤回',
    approver      VARCHAR(64)  DEFAULT NULL COMMENT '审批人',
    remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time   DATETIME     DEFAULT NULL,
    update_time   DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='审批记录表';

-- 工作流实例表
DROP TABLE IF EXISTS wf_instance;
CREATE TABLE wf_instance (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    workflow_id    VARCHAR(64)  NOT NULL COMMENT '工作流ID(UUID)',
    workflow_type  VARCHAR(50)  NOT NULL COMMENT '工作流类型: weekly_report',
    status         VARCHAR(20)  DEFAULT 'RUNNING' COMMENT '状态: RUNNING/SUCCESS/FAILED',
    current_node   VARCHAR(100) DEFAULT NULL COMMENT '当前节点名称',
    context_json   TEXT         DEFAULT NULL COMMENT '上下文数据(JSON)',
    create_by      BIGINT       DEFAULT NULL COMMENT '发起人',
    create_time    DATETIME     DEFAULT NULL,
    update_time    DATETIME     DEFAULT NULL,
    finish_time    DATETIME     DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_workflow_id (workflow_id)
) ENGINE=InnoDB COMMENT='工作流实例表';

-- 工作流任务表
DROP TABLE IF EXISTS wf_task;
CREATE TABLE wf_task (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    workflow_id    VARCHAR(64)  NOT NULL COMMENT '工作流ID',
    node_name      VARCHAR(100) NOT NULL COMMENT '节点名称',
    status         VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAILED',
    input_json     TEXT         DEFAULT NULL COMMENT '输入数据(JSON)',
    output_json    TEXT         DEFAULT NULL COMMENT '输出数据(JSON)',
    error_message  VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    start_time     DATETIME     DEFAULT NULL,
    end_time       DATETIME     DEFAULT NULL,
    duration_ms    INT          DEFAULT 0 COMMENT '执行耗时(ms)',
    PRIMARY KEY (id),
    INDEX idx_workflow_id (workflow_id)
) ENGINE=InnoDB COMMENT='工作流任务表';

-- AI反馈表
DROP TABLE IF EXISTS ai_feedback;
CREATE TABLE ai_feedback (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT       DEFAULT NULL COMMENT '对话ID',
    session_id      VARCHAR(64)  DEFAULT NULL COMMENT '会话ID',
    question        TEXT         DEFAULT NULL COMMENT '用户问题',
    answer          TEXT         DEFAULT NULL COMMENT 'AI回答',
    score           INT          DEFAULT NULL COMMENT '评分1-5',
    comment         VARCHAR(500) DEFAULT NULL COMMENT '用户评语',
    user_id         BIGINT       DEFAULT NULL COMMENT '评价用户',
    create_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='AI反馈表';

-- AI评估表
DROP TABLE IF EXISTS ai_evaluation;
CREATE TABLE ai_evaluation (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    question        TEXT         NOT NULL COMMENT '测试问题',
    expected_answer TEXT         DEFAULT NULL COMMENT '期望答案',
    actual_answer   TEXT         DEFAULT NULL COMMENT '实际回答',
    mode            VARCHAR(20)  DEFAULT NULL COMMENT 'chat/rag/agent',
    response_time   INT          DEFAULT 0 COMMENT '响应时间(ms)',
    tool_count      INT          DEFAULT 0 COMMENT '工具调用次数',
    retrieval_score DOUBLE       DEFAULT 0 COMMENT '检索相关度',
    eval_score      INT          DEFAULT 0 COMMENT '评估得分1-100',
    eval_result     VARCHAR(20)  DEFAULT NULL COMMENT 'PASS/FAIL',
    create_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='AI评估表';

-- Bad Case表
DROP TABLE IF EXISTS ai_bad_case;
CREATE TABLE ai_bad_case (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    question        TEXT         NOT NULL COMMENT '失败问题',
    actual_answer   TEXT         DEFAULT NULL COMMENT '实际回答',
    failure_reason  VARCHAR(500) NOT NULL COMMENT '失败原因: 幻觉/检索失败/工具失败/超时',
    mode            VARCHAR(20)  DEFAULT NULL COMMENT 'rag/agent',
    optimization    VARCHAR(500) DEFAULT NULL COMMENT '优化方案',
    status          VARCHAR(20)  DEFAULT 'OPEN' COMMENT 'OPEN/FIXED/WONTFIX',
    create_time     DATETIME     DEFAULT NULL,
    update_time     DATETIME     DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='Bad Case管理表';

-- 插入项目示例数据
INSERT INTO biz_project (project_name, manager, status, progress, risk, description, create_time) VALUES
('智慧城市数据平台', '张三', '进行中', 65, '中', '城市大数据分析平台建设', sysdate()),
('企业OA系统升级', '李四', '进行中', 30, '低', 'OA系统全面升级', sysdate()),
('移动办公APP', '王五', '已完成', 100, '低', '企业移动办公应用', sysdate()),
('数据安全加固', '张三', '已暂停', 45, '高', '数据安全合规整改', sysdate());

-- 插入审批示例数据
INSERT INTO biz_approval (applicant, type, title, amount, status, approver, create_time) VALUES
('张三', '报销', '差旅费报销-北京出差', 3580.00, '已通过', '李四', sysdate()),
('李四', '请假', '年假申请-国庆前后', NULL, '待审批', '王五', sysdate()),
('王五', '采购', '服务器采购申请', 128000.00, '已拒绝', '张三', sysdate()),
('张三', '出差', '上海客户拜访', NULL, '待审批', '李四', sysdate());

-- 插入AI反馈示例数据
INSERT INTO ai_feedback (session_id, question, answer, score, comment, user_id, create_time) VALUES
('test-s1', '核心工作时间', '10:00-16:00', 5, '回答准确', 1, sysdate()),
('test-s2', '请假流程', '提前1天在OA申请', 4, '基本正确', 1, sysdate());
