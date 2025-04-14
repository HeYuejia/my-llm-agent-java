package com.rag.client;

import com.google.gson.Gson;
import com.rag.utils.LogUtils;

import java.io.IOException;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代理类，负责协调LLM和检索功能
 */
public class Agent {
    private final List<MCPClient> mcpClients;
    private ChatOpenAI llm;
    private final String model;
    private final String systemPrompt;
    private final String context;
    private final Gson gson = new Gson();

    /**
     * 构造函数
     *
     * @param model        模型名称
     * @param mcpClients   MCP客户端列表
     * @param systemPrompt 系统提示
     * @param context      初始上下文
     */
    public Agent(String model, List<MCPClient> mcpClients, String systemPrompt, String context) {
        this.mcpClients = mcpClients;
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.context = context;
    }

    /**
     * 初始化代理
     *
     * @throws IOException 如果初始化失败
     */
    public void init() throws IOException {
        LogUtils.logTitle("TOOLS");
        
        // 初始化所有MCP客户端
        for (MCPClient client : mcpClients) {
            client.init();
        }
        
        // 收集所有工具
        List<Tool> tools = mcpClients.stream()
                .flatMap(client -> client.getTools().stream())
                .collect(Collectors.toList());
        
        // 创建LLM实例
        this.llm = new ChatOpenAI(model, systemPrompt, tools, context);
    }

    /**
     * 关闭代理
     *
     * @throws IOException 如果关闭失败
     */
    public void close() throws IOException {
        for (MCPClient client : mcpClients) {
            client.close();
        }
    }

    /**
     * 处理用户输入
     *
     * @param prompt 用户输入
     * @return LLM响应
     * @throws IOException 如果处理失败
     */
    public String invoke(String prompt) throws IOException {
        if (llm == null) {
            throw new IllegalStateException("Agent not initialized");
        }

        ChatOpenAI.ChatResponse response;

        if(this.context.length() > 8000){
            // 如果上下文过长，生成摘要
            String usedContext = generateContextSummary(this.context);
            response = llm.chatWithContext(prompt, usedContext);
        }
        else
            response = llm.chat(prompt);

        
        while (true) {
            // 处理工具调用
            if (!response.getToolCalls().isEmpty()) {
                for (ChatOpenAI.ToolCall toolCall : response.getToolCalls()) {
                    // 查找对应的MCP客户端
                    MCPClient mcp = findMCPForTool(toolCall.getFunction().getName());
                    
                    if (mcp != null) {
                        LogUtils.logTitle("TOOL USE");
                        System.out.println("Calling tool: " + toolCall.getFunction().getName());
                        System.out.println("Arguments: " + toolCall.getFunction().getArguments());
                        
                        // 调用工具
                        @SuppressWarnings("unchecked")
                        Map<String, Object> arguments = gson.fromJson(
                                toolCall.getFunction().getArguments(), Map.class
                        );
                        Object result = mcp.callTool(toolCall.getFunction().getName(), arguments);
                        
                        System.out.println("Result: " + gson.toJson(result));
                        
                        // 将结果添加到对话历史
                        llm.appendToolResult(toolCall.getId(), gson.toJson(result));
                    } else {
                        llm.appendToolResult(toolCall.getId(), "Tool not found");
                    }
                }
                
                // 继续对话
                response = llm.chat(null);
                continue;
            }
            
            // 没有工具调用，结束对话
            close();
            return response.getContent();
        }
    }

    /**
     * 查找可以处理指定工具的MCP客户端
     *
     * @param toolName 工具名称
     * @return 可以处理该工具的MCP客户端，如果没有找到则返回null
     */
    private MCPClient findMCPForTool(String toolName) {
        for (MCPClient client : mcpClients) {
            if (client.getTools().stream().anyMatch(tool -> tool.getName().equals(toolName))) {
                return client;
            }
        }
        return null;
    }

    private String generateContextSummary(String fullContext) throws IOException {
        // 使用LLM生成上下文摘要
        String summaryPrompt = "请从以下上下文中提取最关键的信息，生成一个简洁的摘要(不超过500字):\n" + fullContext;
        ChatOpenAI.ChatResponse response = llm.chat(summaryPrompt);
        return response.getContent();
    }

    // 在Agent中添加分块处理方法
    private List<String> chunkContext(String context, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int length = context.length();

        //简单按字符/字数分块
//        for (int i = 0; i < length; i += chunkSize) {
//            chunks.add(context.substring(i, Math.min(length, i + chunkSize)));
//        }
        //重叠窗口设计：相邻分块保留部分重叠内容（防止关键信息被切断）
        int overlapSize = 200; // 重叠字符数
        for (int i = 0; i < length; i += chunkSize - overlapSize) {
            chunks.add(context.substring(i, Math.min(length, i + chunkSize)));
        }
        //按语意分块

        return chunks;
    }

    public String invokeWithChunks(String prompt) throws IOException {
        List<String> chunks = chunkContext(this.context, 4000);
        StringBuilder combinedResponse = new StringBuilder();

        for (String chunk : chunks) {
            ChatOpenAI.ChatResponse response = llm.chatWithContext(prompt, chunk);
            combinedResponse.append(response.getContent()).append("\n\n");
        }

        // 合并分块响应
        return llm.chat("合并以下响应:\n" + combinedResponse.toString()).getContent();
    }
} 