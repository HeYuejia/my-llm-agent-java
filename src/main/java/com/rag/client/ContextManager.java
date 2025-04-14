package com.rag.client;

import com.rag.model.EmbeddingRetriever;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

//基于优先级队列的增量式上下文管理
/*
使用示例：
// 初始化
EmbeddingRetriever embedder = new EmbeddingRetriever("BAAI/bge-m3");
ContextManager manager = new ContextManager(5, embedder, "初始对话主题");

// 模拟对话流程
manager.addContext("用户问：什么是RAG?");
manager.addContext("系统答：RAG是检索增强生成...");

// 工具调用更新
manager.updateWithToolResult("知识检索", "在2023年论文中，RAG的准确率提升了40%");

// 获取当前优化后的上下文
List<String> currentContext = manager.getCurrentContext();
/*
可能输出：
1. [Tool Result] 知识检索: 在2023年论文中...(高相关)
2. 系统答：RAG是检索增强生成...(中相关)
3. 用户问：什么是RAG?(低相关)
*/
public class ContextManager {
    // 上下文条目数据结构
    static class ContextEntry {
        String content;
        double[] embedding;
        double priority; // 动态优先级分数
        long lastAccessed; // 最后访问时间戳

        public ContextEntry(String content, double[] embedding) {
            this.content = content;
            this.embedding = embedding;
            this.priority = 1.0; // 初始优先级
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    private final PriorityQueue<ContextEntry> contextQueue;
    private final int maxContextSize; // 最大上下文条目数
    private final EmbeddingRetriever embeddingRetriever;
    private double[] currentFocusEmbedding; // 当前对话焦点嵌入

    public ContextManager(int maxContextSize, EmbeddingRetriever embeddingRetriever, String initialFocus) 
        throws IOException {
        this.maxContextSize = maxContextSize;
        this.embeddingRetriever = embeddingRetriever;
        this.currentFocusEmbedding = embeddingRetriever.embedQuery(initialFocus);
        this.contextQueue = new PriorityQueue<>(
            Comparator.comparingDouble(e -> e.priority) // 低优先级在前
        );
    }

    /**
     * 添加新上下文内容（自动维护队列大小）
     */
    public synchronized void addContext(String content) throws IOException {
        double[] embedding = embeddingRetriever.embedQuery(content);
        ContextEntry newEntry = new ContextEntry(content, embedding);
        
        // 计算初始优先级（基于与当前焦点的相关性）
        updateEntryPriority(newEntry);
        
        // 维护队列大小
        if (contextQueue.size() >= maxContextSize) {
            contextQueue.poll(); // 移除优先级最低的
        }
        contextQueue.offer(newEntry);
    }

    /**
     * 根据工具调用结果更新上下文
     */
    public synchronized void updateWithToolResult(String toolName, String result) throws IOException {
        // 1. 将工具结果作为高优先级上下文添加
        addContext("[Tool Result] " + toolName + ": " + result);
        
        // 2. 更新当前焦点（假设工具结果反映了当前关注点）
        this.currentFocusEmbedding = embeddingRetriever.embedQuery(result);
        
        // 3. 重新计算所有条目的优先级
        recomputePriorities();
    }

    /**
     * 获取当前上下文（按优先级排序）
     */
    public synchronized List<String> getCurrentContext() {
        List<ContextEntry> entries = new ArrayList<>(contextQueue);
        entries.sort(Comparator.comparingDouble(e -> -e.priority)); // 降序
        
        return entries.stream()
            .map(e -> e.content)
            .collect(Collectors.toList());
    }

    /**
     * 重新计算所有条目的优先级
     */
    private void recomputePriorities() {
        PriorityQueue<ContextEntry> newQueue = new PriorityQueue<>(
            Comparator.comparingDouble(e -> e.priority)
        );
        
        for (ContextEntry entry : contextQueue) {
            updateEntryPriority(entry);
            newQueue.offer(entry);
        }
        
        contextQueue.clear();
        contextQueue.addAll(newQueue);
    }

    /**
     * 更新单个条目的优先级
     */
    private void updateEntryPriority(ContextEntry entry) {
        try {
            // 1. 计算与当前焦点的语义相关性（0-1）
            double relevance = cosineSimilarity(currentFocusEmbedding, entry.embedding);
            
            // 2. 时间衰减因子（最近使用的权重更高）
            double timeDecay = Math.exp(
                -0.0001 * (System.currentTimeMillis() - entry.lastAccessed)
            );
            
            // 3. 综合优先级计算
            entry.priority = 0.7 * relevance 
                          + 0.2 * timeDecay
                          + 0.1 * (1.0 - (entry.content.length() / 1000.0)); // 适度长度有利
            
            entry.lastAccessed = System.currentTimeMillis();
        } catch (Exception e) {
            entry.priority = 0.1; // 计算失败时设为低优先级
        }
    }

    // 余弦相似度计算（同前）
    private double cosineSimilarity(double[] vecA, double[] vecB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // 根据用户输入自动检测焦点变化
    void detectFocusShift(String newInput) throws IOException {
        double[] newEmbedding = new EmbeddingRetriever("BAAI/bge-m3").embedQuery(newInput);
        if (cosineSimilarity(currentFocusEmbedding, newEmbedding) < 0.6) {
            this.currentFocusEmbedding = newEmbedding;
            recomputePriorities(); // 焦点变化时重算优先级
        }
    }
}