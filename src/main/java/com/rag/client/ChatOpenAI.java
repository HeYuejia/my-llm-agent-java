package com.rag.client;

import com.google.gson.Gson;
import com.rag.utils.LogUtils;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.service.OpenAiService;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具调用信息类
 */
public class ChatOpenAI {
    public ChatResponse chatWithContext(String prompt, String usedContext) {
        //TODO
        return null;
    }

    private final Map<String, ChatResponse> responseCache = new LinkedHashMap<>(100, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ChatResponse> eldest) {
            return size() > 100; // 限制缓存大小
        }
    };

    public static class ToolCall {
        private String id;
        private Function function;

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.function = new Function(name, arguments);
        }

        public String getId() {
            return id;
        }

        public Function getFunction() {
            return function;
        }

        public static class Function {
            private String name;
            private String arguments;

            public Function(String name, String arguments) {
                this.name = name;
                this.arguments = arguments;
            }

            public String getName() {
                return name;
            }

            public String getArguments() {
                return arguments;
            }
        }
    }

    private final OpenAiService openAiService;
    private final String model;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<ChatCompletionTool> tools;
    private final Gson gson = new Gson();

    /**
     * 构造函数
     *
     * @param model        OpenAI模型名称
     * @param systemPrompt 系统提示
     * @param tools        可用工具列表
     * @param context      初始上下文
     */
    public ChatOpenAI(String model, String systemPrompt, List<Tool> tools, String context) {
        Dotenv dotenv = Dotenv.load();
        String apiKey = dotenv.get("OPENAI_API_KEY");
        String baseUrl = dotenv.get("OPENAI_BASE_URL");
        
        this.openAiService = new OpenAiService(apiKey, baseUrl);
        this.model = model;
        this.tools = convertTools(tools);
        
        if (!systemPrompt.isEmpty()) {
            messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(), systemPrompt));
        }
        
        if (!context.isEmpty()) {
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), context));
        }
    }

    /**
     * 将MCP工具转换为OpenAI工具格式
     *
     * @param mcpTools MCP工具列表
     * @return OpenAI工具列表
     */
    private List<ChatCompletionTool> convertTools(List<Tool> mcpTools) {
        List<ChatCompletionTool> openAiTools = new ArrayList<>();
        
        for (Tool tool : mcpTools) {
            ChatCompletionFunctionParameters parameters = new ChatCompletionFunctionParameters();
            parameters.setSchema(tool.getInputSchema());
            
            ChatCompletionFunction function = new ChatCompletionFunction();
            function.setName(tool.getName());
            function.setDescription(tool.getDescription());
            function.setParameters(parameters);
            
            ChatCompletionTool chatTool = new ChatCompletionTool();
            chatTool.setType("function");
            chatTool.setFunction(function);
            
            openAiTools.add(chatTool);
        }
        
        return openAiTools;
    }

    /**
     * 发送消息到OpenAI并获取回复
     *
     * @param prompt 用户输入，如果为null则继续之前的对话
     * @return 包含内容和工具调用的响应
     */
    public ChatResponse chat(String prompt) throws OpenAiHttpException {
        LogUtils.logTitle("CHAT");

        String cacheKey = generateCacheKey(prompt);
        if (responseCache.containsKey(cacheKey)) {
            return responseCache.get(cacheKey);
        }
        
        if (prompt != null && !prompt.isEmpty()) {
            messages.add(new ChatMessage(ChatMessageRole.USER.value(), prompt));
        }
        
        ChatCompletionRequest.Builder requestBuilder = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages);
        
        if (!tools.isEmpty()) {
            requestBuilder.tools(tools);
        }
        
        ChatCompletionRequest request = requestBuilder.build();
        
        LogUtils.logTitle("RESPONSE");
        
        try {
            ChatCompletion completion = openAiService.createChatCompletion(request);
            ChatCompletionChoice choice = completion.getChoices().get(0);
            ChatMessage responseMessage = choice.getMessage();
            
            // 获取内容和工具调用
            String content = responseMessage.getContent() != null ? responseMessage.getContent() : "";
            List<ToolCall> toolCalls = new ArrayList<>();
            
            if (responseMessage.getToolCalls() != null) {
                for (ChatCompletionMessageToolCall toolCall : responseMessage.getToolCalls()) {
                    toolCalls.add(new ToolCall(
                            toolCall.getId(),
                            toolCall.getFunction().getName(),
                            toolCall.getFunction().getArguments()
                    ));
                }
            }
            
            // 将响应添加到消息历史
            messages.add(responseMessage);

            ChatResponse response = new ChatResponse(content, toolCalls);
            // 存入缓存
            responseCache.put(cacheKey, response);
            return response;
        } catch (OpenAiHttpException e) {
            System.err.println("OpenAI API error: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 将工具调用结果添加到对话历史
     *
     * @param toolCallId 工具调用ID
     * @param result     工具调用结果
     */
    public void appendToolResult(String toolCallId, String result) {
        messages.add(new ChatMessage(
                ChatMessageRole.TOOL.value(),
                result,
                null,
                toolCallId
        ));
    }

    /**
     * 聊天响应类，包含内容和工具调用
     */
    public static class ChatResponse {
        private final String content;
        private final List<ToolCall> toolCalls;

        public ChatResponse(String content, List<ToolCall> toolCalls) {
            this.content = content;
            this.toolCalls = toolCalls;
        }

        public String getContent() {
            return content;
        }

        public List<ToolCall> getToolCalls() {
            return toolCalls;
        }
    }
}

/**
 * 工具类，表示一个可以被调用的函数
 */
class Tool {
    private final String name;
    private final String description;
    private final Map<String, Object> inputSchema;

    public Tool(String name, String description, Map<String, Object> inputSchema) {
        this.name = name;
        this.description = description;
        this.inputSchema = inputSchema;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getInputSchema() {
        return inputSchema;
    }

    private String generateCacheKey(String prompt) {
        // 生成基于内容和上下文的缓存键
        return Integer.toHexString((prompt.toString() + prompt).hashCode());//TODO:prompt.toString()应该是context.toString()
    }
} 