package com.ruoyi.ai.tool;

/**
 * Agent 工具接口 —— 所有工具必须实现此接口
 *
 * 面试知识点：
 * 策略模式 + 注解扫描 = 开放封闭原则（对扩展开放，对修改封闭）
 * 新增工具只需：1. 实现 AiTool 接口  2. 加 @AgentTool 注解  无需修改任何已有代码
 */
public interface AiTool {

    /** 执行工具 */
    String execute(String argsJson);
}
