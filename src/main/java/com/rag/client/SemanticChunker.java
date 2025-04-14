package com.rag.client;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticChunker {
    
    // 分块配置参数
    private static final int MIN_CHUNK_SIZE = 200;  // 最小分块字符数
    private static final int MAX_CHUNK_SIZE = 800;  // 最大分块字符数
    private static final int OVERLAP_SIZE = 100;    // 重叠区域大小
    
    // 语义边界正则表达式
    private static final Pattern SEMANTIC_BOUNDARIES = Pattern.compile(
        "(\\n\\s*\\n)|(?<=[.!?])\\s+|(?<=\\n#{1,6}\\s)|(?<=\\*\\*\\*)|(?<=====)",
        Pattern.MULTILINE
    );
    
    /**
     * 基于语义的文本分块方法
     * @param text 输入文本
     * @return 分块后的文本列表
     */
    public static List<String> chunkContext(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        
        // 使用语义边界分割文本
        List<Integer> splitPoints = findSplitPoints(text);
        splitPoints.add(0, 0);
        splitPoints.add(text.length());
        
        // 构建初始分块
        List<String> initialChunks = new ArrayList<>();
        for (int i = 1; i < splitPoints.size(); i++) {
            int start = splitPoints.get(i-1);
            int end = splitPoints.get(i);
            initialChunks.add(text.substring(start, end).trim());
        }
        
        // 合并过小的分块
        chunks = mergeSmallChunks(initialChunks);
        
        // 确保分块不超过最大大小
        chunks = splitLargeChunks(chunks);
        
        return chunks;
    }
    
    /**
     * 查找语义分割点
     */
    private static List<Integer> findSplitPoints(String text) {
        List<Integer> points = new ArrayList<>();
        Matcher matcher = SEMANTIC_BOUNDARIES.matcher(text);
        
        while (matcher.find()) {
            points.add(matcher.end());
        }
        return points;
    }
    
    /**
     * 合并过小的分块
     */
    private static List<String> mergeSmallChunks(List<String> chunks) {
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        for (String chunk : chunks) {
            current.append(chunk).append("\n\n");
            
            if (current.length() >= MIN_CHUNK_SIZE) {
                merged.add(current.toString().trim());
                current = new StringBuilder();
                // 添加重叠区域
                if (!chunk.isEmpty()) {
                    int overlapStart = Math.max(0, chunk.length() - OVERLAP_SIZE);
                    current.append(chunk.substring(overlapStart));
                }
            }
        }
        
        // 添加最后剩余部分
        String s = current.toString();
        if (!s.isEmpty()) {
            merged.add(s.trim());
        }
        
        return merged;
    }
    
    /**
     * 分割过大的分块
     */
    private static List<String> splitLargeChunks(List<String> chunks) {
        List<String> result = new ArrayList<>();
        
        for (String chunk : chunks) {
            if (chunk.length() <= MAX_CHUNK_SIZE) {
                result.add(chunk);
                continue;
            }
            
            // 对过大的块按句子二次分割
            String[] sentences = chunk.split("(?<=[.!?])\\s+");
            StringBuilder current = new StringBuilder();
            
            for (String sentence : sentences) {
                if (current.length() + sentence.length() > MAX_CHUNK_SIZE) {
                    result.add(current.toString().trim());
                    current = new StringBuilder();
                    // 保留部分上文的上下文
                    int overlapStart = Math.max(0, current.length() - OVERLAP_SIZE);
                    if (overlapStart > 0) {
                        current.append(current.substring(overlapStart));
                    }
                }
                current.append(sentence).append(" ");
            }

            String s = current.toString();
            if (!s.isEmpty()) {
                result.add(s.trim());
            }
        }
        
        return result;
    }
}