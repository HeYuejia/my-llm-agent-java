package com.rag.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rag.utils.LogUtils;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;
import java.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 嵌入检索器，负责获取文档嵌入和检索相关文档
 */
public class EmbeddingRetriever {
    private final String embeddingModel;
    private final VectorStore vectorStore;
    private final String embeddingBaseUrl;
    private final String embeddingKey;
    private final OkHttpClient httpClient;
    private final Map<String, double[]> embeddingCache = new HashMap<>();
    public EmbeddingRetriever(String embeddingModel) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = new VectorStore();
        
        Dotenv dotenv = Dotenv.load();
        this.embeddingBaseUrl = dotenv.get("EMBEDDING_BASE_URL");
        this.embeddingKey = dotenv.get("EMBEDDING_KEY");
        this.httpClient = new OkHttpClient();
    }

    /**
     * 为文档生成嵌入并存储
     *
     * @param document 文档内容
     * @return 嵌入向量
     * @throws IOException 如果API调用失败
     */
    public double[] embedDocument(String document) throws IOException {
        LogUtils.logTitle("EMBEDDING DOCUMENT");
        double[] embedding = embed(document);
        vectorStore.addEmbedding(embedding, document);
        return embedding;
    }

    /**
     * 为查询生成嵌入
     *
     * @param query 查询文本
     * @return 嵌入向量
     * @throws IOException 如果API调用失败
     */
    public double[] embedQuery(String query) throws IOException {
        LogUtils.logTitle("EMBEDDING QUERY");
        return embed(query);
    }

    /**
     * 调用嵌入API生成文本的嵌入向量
     *
     * @param text 需要嵌入的文本
     * @return 嵌入向量
     * @throws IOException 如果API调用失败
     */
    private double[] embed(String text) throws IOException {
        // 检查缓存
        String hash = Integer.toHexString(text.hashCode());
        if (embeddingCache.containsKey(hash)) {
            return embeddingCache.get(hash);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", embeddingModel);
        requestBody.addProperty("input", text);
        requestBody.addProperty("encoding_format", "float");

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(embeddingBaseUrl + "/embeddings")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + embeddingKey)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray dataArray = jsonResponse.getAsJsonArray("data");
            JsonObject firstData = dataArray.get(0).getAsJsonObject();
            JsonArray embeddingArray = firstData.getAsJsonArray("embedding");

            double[] embedding = new double[embeddingArray.size()];
            for (int i = 0; i < embeddingArray.size(); i++) {
                embedding[i] = embeddingArray.get(i).getAsDouble();
            }

            System.out.println(embeddingArray);
            // 存入缓存
            embeddingCache.put(hash, embedding);
            return embedding;
        }
    }

    /**
     * 检索与查询最相关的文档
     *
     * @param query 查询文本
     * @param topK  返回的最大文档数量
     * @return 相关度最高的topK个文档
     * @throws IOException 如果API调用失败
     */
    public List<String> retrieve(String query, int topK) throws IOException {
        double[] queryEmbedding = embedQuery(query);
        return vectorStore.search(queryEmbedding, topK);
    }
} 