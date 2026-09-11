package com.ruoyi.ai.tool;

import java.lang.annotation.*;

/**
 * Agent工具标记注解
 *
 * 标注在 Tool 实现类上，ToolRegistry 自动扫描并注册。
 * 面试知识点：注解 + 反射实现工具自动注册，比 switch-case 更优雅、更易扩展。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AgentTool {
    /** 工具名称（Function Calling 中的 function.name） */
    String name();
    /** 工具描述（LLM 根据描述决定何时调用） */
    String description();
}
