package com.rag;

import com.rag.client.Agent;
import com.rag.client.MCPClient;
import com.rag.model.EmbeddingRetriever;
import com.rag.utils.LogUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主类，程序入口点
 */
public class Main {
    // 常量配置
    private static final String URL = "https://news.ycombinator.com/";
    private static final String OUTPUT_DIR = System.getProperty("user.dir") + "/output";
    private static final String TASK = "\n" +
            "告诉我Antonette的信息,先从我给你的context中找到相关信息,总结后创作一个关于她的故事\n" +
            "把故事和她的基本信息保存到" + OUTPUT_DIR + "/antonette.md,输出一个漂亮md文件\n";

    public static void main(String[] args) {
        try {
            // 创建输出目录
            createOutputDirectory();
            
            // 检索相关上下文
            String context = retrieveContext();

            /*
            流程：
            创建输出目录
            构造检索上下文：
                创建嵌入检索器，使用的是BAAI/bge-m3模型
                从/knowledge目录读取本地知识库
                对本地知识库中的每个文件进行嵌入embeddingRetriever.embedDocument(content)：
                    对输入字符串调用嵌入api，计算出对应的一个向量
                    将该向量和其文本字符串本身以kv形式存在VectorStore中
                构建好向量数据库以后，检索相关文档embeddingRetriever.retrieve(TASK, 3)获取上下文：
                    将查询字符串向量化得到queryEmbedding
                    将queryEmbedding与topK参数一起传入vectorStore.search方法中，根据查询向量搜索最相似的文档：
                        遍历向量数据库，对每一条记录计算它与查询向量的余弦相似度
                        收集相似度排位topK的记录返回
            创建MCP客户端：
                创建支持网络获取的客户端
                创建支持操作文件的客户端
            创建agent（将本地知识库作为上下文传入）
            初始化agent：
                初始化所有MCP客户端
                从MCP客户端中收集所有工具tool
                创建LLM实例
            执行任务invoke：
                发送用户输入到LLM并等待response（会根据上下文长度判断一下要不要生成摘要）
                进入死循环：
                    处理工具调用，如果没有工具调用的话，结束会话，退出循环
                    有的话，遍历response.getToolCalls()得到的每一个工具调用：
                        查找对应的MCP客户端
                        调用工具
                        将结果添加到对话历史llm.appendToolResult
                    继续下一次对话
            关闭agent 关闭所有client
             */
            // 创建MCP客户端
            MCPClient fetchMCP = new MCPClient("mcp-server-fetch", "uvx", new String[]{"mcp-server-fetch"});
            MCPClient fileMCP = new MCPClient("mcp-server-file", "npx", 
                    new String[]{"-y", "@modelcontextprotocol/server-filesystem", OUTPUT_DIR});
            List<MCPClient> mcpClients = Arrays.asList(fetchMCP, fileMCP);
            
            // 创建代理
            Agent agent = new Agent("openai/gpt-4o-mini", mcpClients, "", context);
            
            // 初始化代理
            agent.init();
            
            // 执行任务
            String result = agent.invoke(TASK);
            System.out.println("Task completed with result: " + result);
            
            // 关闭代理
            agent.close();
            
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建输出目录
     */
    private static void createOutputDirectory() {
        File outputDir = new File(OUTPUT_DIR);
        if (!outputDir.exists()) {
            if (outputDir.mkdirs()) {
                System.out.println("Created output directory: " + OUTPUT_DIR);
            } else {
                System.err.println("Failed to create output directory: " + OUTPUT_DIR);
            }
        }
    }

    /**
     * 检索相关上下文
     *
     * @return 相关上下文
     * @throws IOException 如果检索失败
     */
    private static String retrieveContext() throws IOException {
        // 创建嵌入检索器
        EmbeddingRetriever embeddingRetriever = new EmbeddingRetriever("BAAI/bge-m3");
        
        // 读取知识库文件
        String knowledgeDir = System.getProperty("user.dir") + "/knowledge";
        File knowledgeFolder = new File(knowledgeDir);
        
        if (!knowledgeFolder.exists() || !knowledgeFolder.isDirectory()) {
            throw new IOException("Knowledge directory not found: " + knowledgeDir);
        }
        
        // 对每个文件进行嵌入
        File[] files = knowledgeFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                    embeddingRetriever.embedDocument(content);
                }
            }
        }
        
        // 检索相关文档
        List<String> relevantDocs = embeddingRetriever.retrieve(TASK, 3);
        String context = String.join("\n", relevantDocs);
        
        // 打印上下文
        LogUtils.logTitle("CONTEXT");
        System.out.println(context);
        
        return context;
    }
} 