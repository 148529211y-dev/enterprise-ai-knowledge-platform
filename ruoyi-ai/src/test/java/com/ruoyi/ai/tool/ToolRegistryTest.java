package com.ruoyi.ai.tool;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.ai.exception.ToolExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryTest {

    @AgentTool(name = "echo", description = "Echoes input back")
    static class EchoTool implements AiTool {
        @Override
        public String execute(String argsJson) {
            JSONObject args = JSONObject.parseObject(argsJson);
            return args.getString("text");
        }

        @Override
        public JSONObject getParametersSchema() {
            JSONObject schema = new JSONObject();
            schema.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject textProp = new JSONObject();
            textProp.put("type", "string");
            props.put("text", textProp);
            schema.put("properties", props);
            return schema;
        }
    }

    @AgentTool(name = "add", description = "Adds two numbers")
    static class AddTool implements AiTool {
        @Override
        public String execute(String argsJson) {
            JSONObject args = JSONObject.parseObject(argsJson);
            int a = args.getIntValue("a");
            int b = args.getIntValue("b");
            return String.valueOf(a + b);
        }

        @Override
        public JSONObject getParametersSchema() {
            JSONObject schema = new JSONObject();
            schema.put("type", "object");
            JSONObject props = new JSONObject();
            JSONObject aProp = new JSONObject();
            aProp.put("type", "integer");
            JSONObject bProp = new JSONObject();
            bProp.put("type", "integer");
            props.put("a", aProp);
            props.put("b", bProp);
            schema.put("properties", props);
            return schema;
        }
    }

    static class NoAnnotationTool implements AiTool {
        @Override
        public String execute(String argsJson) {
            return "hidden";
        }
    }

    private EchoTool echoTool;
    private AddTool addTool;

    @BeforeEach
    void setUp() {
        echoTool = new EchoTool();
        addTool = new AddTool();
    }

    @Test
    void init_registersAnnotatedTools() {
        List<AiTool> tools = Arrays.asList(echoTool, addTool);
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        Set<String> names = registry.getToolNames();
        assertEquals(2, names.size());
        assertTrue(names.contains("echo"));
        assertTrue(names.contains("add"));
    }

    @Test
    void init_ignoresToolsWithoutAnnotation() {
        List<AiTool> tools = Arrays.asList(echoTool, new NoAnnotationTool());
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        assertEquals(1, registry.getToolNames().size());
        assertTrue(registry.getToolNames().contains("echo"));
    }

    @Test
    void getToolsSchema_returnsCorrectJson() {
        List<AiTool> tools = Collections.singletonList(echoTool);
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        JSONArray schema = registry.getToolsSchema();
        assertEquals(1, schema.size());

        JSONObject toolDef = schema.getJSONObject(0);
        assertEquals("function", toolDef.getString("type"));

        JSONObject function = toolDef.getJSONObject("function");
        assertEquals("echo", function.getString("name"));
        assertEquals("Echoes input back", function.getString("description"));
        assertNotNull(function.getJSONObject("parameters"));
    }

    @Test
    void getToolsSchema_multipleTools_returnsAll() {
        List<AiTool> tools = Arrays.asList(echoTool, addTool);
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        JSONArray schema = registry.getToolsSchema();
        assertEquals(2, schema.size());
    }

    @Test
    void executeTool_executesCorrectTool() {
        List<AiTool> tools = Arrays.asList(echoTool, addTool);
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        String result = registry.executeTool("echo", "{\"text\":\"hello\"}");
        assertEquals("hello", result);

        String sum = registry.executeTool("add", "{\"a\":3,\"b\":5}");
        assertEquals("8", sum);
    }

    @Test
    void executeTool_unknownTool_throwsToolExecutionException() {
        List<AiTool> tools = Collections.singletonList(echoTool);
        ToolRegistry registry = new ToolRegistry(tools);
        registry.init();

        assertThrows(ToolExecutionException.class,
                () -> registry.executeTool("unknown", "{}"));
    }

    @Test
    void getToolsSchema_emptyRegistry_returnsEmptyArray() {
        ToolRegistry registry = new ToolRegistry(Collections.emptyList());
        registry.init();

        JSONArray schema = registry.getToolsSchema();
        assertEquals(0, schema.size());
    }
}
