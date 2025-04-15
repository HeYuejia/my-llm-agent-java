package com.rag.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Model Context Protocol 客户端，用于与MCP服务器通信
 */
public class MCPClient {
    private final String name;
    private final String command;
    private final String[] args;
    private final String version;
    private Process process;
    private PrintWriter writer;
    private BufferedReader reader;
    private final Gson gson = new Gson();
    private final List<Tool> tools = new ArrayList<>();

    /**
     * 构造函数
     *
     * @param name    客户端名称
     * @param command 命令
     * @param args    命令参数
     * @param version 版本号（可选）
     */
    public MCPClient(String name, String command, String[] args, String version) {
        this.name = name;
        this.command = command;
        this.args = args;
        this.version = version != null ? version : "0.0.1";
    }

    /**
     * 构造函数（使用默认版本号）
     *
     * @param name    客户端名称
     * @param command 命令
     * @param args    命令参数
     */
    public MCPClient(String name, String command, String[] args) {
        this(name, command, args, "0.0.1");
    }

    /**
     * 初始化MCP客户端
     *
     * @throws IOException 如果连接失败
     */
    public void init() throws IOException {
        connectToServer();
    }

    /**
     * 关闭MCP客户端连接
     */
    public void close() {
        if (process != null && process.isAlive()) {
            writer.close();
            try {
                reader.close();
                process.destroy();
                process.waitFor(5, TimeUnit.SECONDS);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (Exception e) {
                System.err.println("Error closing MCP client: " + e.getMessage());
            }
        }
    }

    /**
     * 获取可用工具列表
     *
     * @return 工具列表
     */
    public List<Tool> getTools() {
        return tools;
    }

    /**
     * 调用工具
     *
     * @param name   工具名称
     * @param params 参数
     * @return 工具调用结果
     * @throws IOException 如果调用失败
     */
    public Object callTool(String name, Map<String, Object> params) throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("jsonrpc", "2.0");
        request.addProperty("method", "callTool");
        request.addProperty("id", 1); // 简化处理，使用固定ID
        
        JsonObject paramsObj = new JsonObject();
        paramsObj.addProperty("name", name);
        paramsObj.add("arguments", gson.toJsonTree(params));
        request.add("params", paramsObj);
        
        String requestStr = gson.toJson(request);
        writer.println(requestStr);
        writer.flush();
        
        String response = reader.readLine();
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        
        // 检查是否有错误
        if (jsonResponse.has("error")) {
            throw new IOException("Error calling tool: " + jsonResponse.get("error").toString());
        }
        
        return gson.fromJson(jsonResponse.get("result"), Object.class);
    }

    /**
     * 连接到MCP服务器
     *
     * @throws IOException 如果连接失败
     */
    private void connectToServer() throws IOException {
        try {
            //用于创建进程的变量
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    processBuilder.command().add(arg);
                }
            }

            //启动这个与服务器进行连接的进程并建立其连接通道用于读写
            process = processBuilder.start();
            writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()), true);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            // 初始化连接
            JsonObject initRequest = new JsonObject();
            initRequest.addProperty("jsonrpc", "2.0");
            initRequest.addProperty("method", "initialize");
            initRequest.addProperty("id", 0);
            
            JsonObject initParams = new JsonObject();
            initParams.addProperty("name", name);
            initParams.addProperty("version", version);
            initRequest.add("params", initParams);
            
            writer.println(gson.toJson(initRequest));
            writer.flush();
            
            String initResponse = reader.readLine();
            // 读取初始化响应
            
            // 获取工具列表
            JsonObject listToolsRequest = new JsonObject();
            listToolsRequest.addProperty("jsonrpc", "2.0");
            listToolsRequest.addProperty("method", "listTools");
            listToolsRequest.addProperty("id", 1);
            
            writer.println(gson.toJson(listToolsRequest));
            writer.flush();
            
            String toolsResponse = reader.readLine();
            JsonObject toolsJson = JsonParser.parseString(toolsResponse).getAsJsonObject();
            
            if (toolsJson.has("result") && toolsJson.getAsJsonObject("result").has("tools")) {
                for (var tool : toolsJson.getAsJsonObject("result").getAsJsonArray("tools")) {
                    JsonObject toolObj = tool.getAsJsonObject();
                    String toolName = toolObj.get("name").getAsString();
                    String description = toolObj.has("description") ? 
                            toolObj.get("description").getAsString() : "";
                    Map<String, Object> inputSchema = toolObj.has("inputSchema") ? 
                            gson.fromJson(toolObj.get("inputSchema"), Map.class) : new HashMap<>();
                    
                    tools.add(new Tool(toolName, description, inputSchema));
                }
            }
            
            System.out.println("Connected to server with tools: " + 
                    tools.stream().map(Tool::getName).collect(Collectors.toList()));
            
        } catch (Exception e) {
            System.err.println("Failed to connect to MCP server: " + e.getMessage());
            throw new IOException("Connection failed", e);
        }
    }
} 