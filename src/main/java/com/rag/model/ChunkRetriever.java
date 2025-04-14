package com.rag.model;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


//简单的分块检索优化方案，包含嵌入缓存、相关性排序和结果精炼
/*
使用示例：
List<String> chunks = SemanticChunker.chunkText(longText);
EmbeddingRetriever embedder = new EmbeddingRetriever("BAAI/bge-m3");
ChunkRetriever retriever = new ChunkRetriever(chunks, embedder);

// 执行查询
List<TextChunk> results = retriever.retrieveOptimized("Antonette的设计原理", 3);

// 输出结果
for (TextChunk chunk : results) {
    System.out.printf("【相关度: %.2f】分块%d (长度:%d)%n%s%n%n",
        retriever.cosineSimilarity(queryEmbedding, chunk.embedding),
        chunk.seqNumber,
        chunk.content.length(),
        chunk.content.substring(0, Math.min(200, chunk.content.length())));
}
 */

public class ChunkRetriever {
    private final List<TextChunk> chunks;
    private final Map<String, double[]> embeddingCache;
    private final EmbeddingRetriever embeddingRetriever;

    public ChunkRetriever(List<String> chunks, EmbeddingRetriever embeddingRetriever) {
        this.embeddingRetriever = embeddingRetriever;
        this.embeddingCache = new HashMap<>();
        
        // 初始化分块并预计算嵌入
        this.chunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String content = chunks.get(i);
            TextChunk chunk = new TextChunk(
                "chunk_" + i,
                content,
                i,
                calculateEmbedding(content)  // 嵌入计算（带缓存）
            );
            this.chunks.add(chunk);
        }
    }

    /**
     * 带缓存的嵌入计算
     */
    private double[] calculateEmbedding(String text) {
        String hash = Integer.toHexString(text.hashCode());
        if (embeddingCache.containsKey(hash)) {
            return embeddingCache.get(hash);
        }
        
        try {
            double[] embedding = embeddingRetriever.embedQuery(text);
            embeddingCache.put(hash, embedding);
            return embedding;
        } catch (IOException e) {
            throw new RuntimeException("Embedding计算失败", e);
        }
    }

    /**
     * 检索最相关的分块（基础版）
     */
    public List<TextChunk> retrieve(String query, int topK) {
        try {
            double[] queryEmbedding = embeddingRetriever.embedQuery(query);
            
            // 计算相关性并排序
            return chunks.stream()
                .map(chunk -> new ScoredChunk(
                    chunk,
                    cosineSimilarity(queryEmbedding, chunk.embedding)
                ))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(ScoredChunk::chunk)
                .collect(Collectors.toList());
            
        } catch (IOException e) {
            throw new RuntimeException("检索失败", e);
        }
    }

    /**
     * 检索优化版 - 带二级精炼
     */
    public List<TextChunk> retrieveOptimized(String query, int topK) {
        // 第一级：快速筛选候选分块
        List<TextChunk> candidates = retrieve(query, Math.min(topK * 3, chunks.size()));
        
        // 第二级：精炼结果（考虑分块位置和长度）
        return candidates.stream()
            .map(chunk -> {
                try {
                    return new ScoredChunk(
                        chunk,
                        refinedScore(
                            cosineSimilarity(embeddingRetriever.embedQuery(query), chunk.embedding),
                            chunk.seqNumber,
                            chunk.content.length()
                        )
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
            .limit(topK)
            .map(ScoredChunk::chunk)
            .collect(Collectors.toList());
    }

    // 精炼评分公式
    private double refinedScore(double similarity, int position, int length) {
        double positionWeight = 1.0 / (1 + Math.log(1 + position)); // 靠前的分块权重高
        double lengthWeight = Math.min(1.0, length / 500.0);       // 适度长度有利
        return similarity * 0.7 + positionWeight * 0.2 + lengthWeight * 0.1;
    }

    // 余弦相似度计算
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

    // 分块数据结构
    static class TextChunk {
        String id;
        String content;
        int seqNumber;
        double[] embedding;

        public TextChunk(String id, String content, int seqNumber, double[] embedding) {
            this.id = id;
            this.content = content;
            this.seqNumber = seqNumber;
            this.embedding = embedding;
        }
    }

    // 带评分分块
    record ScoredChunk(TextChunk chunk, double score) {}
}