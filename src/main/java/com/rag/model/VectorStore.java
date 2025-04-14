package com.rag.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量存储项，包含嵌入向量和对应的文档
 */
class VectorStoreItem {
    private double[] embedding;
    private String document;

    public VectorStoreItem(double[] embedding, String document) {
        this.embedding = embedding;
        this.document = document;
    }

    public double[] getEmbedding() {
        return embedding;
    }

    public String getDocument() {
        return document;
    }
}

/**
 * 向量存储类，负责存储和检索向量嵌入
 */
public class VectorStore {
    private List<VectorStoreItem> vectorStore;

    public VectorStore() {
        this.vectorStore = new ArrayList<>();
    }

    /**
     * 添加向量嵌入到存储中
     *
     * @param embedding 向量嵌入
     * @param document  对应的文档
     */
    public void addEmbedding(double[] embedding, String document) {
        vectorStore.add(new VectorStoreItem(embedding, document));
    }

    /**
     * 根据查询向量搜索最相似的文档
     *
     * @param queryEmbedding 查询向量
     * @param topK           返回的最大文档数量
     * @return 相似度最高的topK个文档
     */
    public List<String> search(double[] queryEmbedding, int topK) {
        List<ScoredDocument> scored = new ArrayList<>();
        
        for (VectorStoreItem item : vectorStore) {
            double score = cosineSimilarity(queryEmbedding, item.getEmbedding());
            scored.add(new ScoredDocument(item.getDocument(), score));
        }
        
        return scored.stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::getScore).reversed())
                .limit(topK)
                .map(ScoredDocument::getDocument)
                .collect(Collectors.toList());
    }

    /**
     * 计算两个向量之间的余弦相似度
     *
     * @param vecA 向量A
     * @param vecB 向量B
     * @return 余弦相似度值
     */
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
    
    /**
     * 带有分数的文档，用于排序
     */
    private static class ScoredDocument {
        private String document;
        private double score;
        
        public ScoredDocument(String document, double score) {
            this.document = document;
            this.score = score;
        }
        
        public String getDocument() {
            return document;
        }
        
        public double getScore() {
            return score;
        }
    }
} 