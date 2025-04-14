package com.rag.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类，提供格式化日志输出功能
 */
public class LogUtils {
    private static final Logger logger = LoggerFactory.getLogger(LogUtils.class);
    private static final int TOTAL_LENGTH = 80;

    /**
     * 打印带有标题样式的日志
     *
     * @param message 要打印的消息
     */
    public static void logTitle(String message) {
        int messageLength = message.length();
        int padding = Math.max(0, TOTAL_LENGTH - messageLength - 4); // 4 for the "=="
        String paddedMessage = "=" .repeat(padding / 2) + " " + message + " " + "=".repeat((padding + 1) / 2);
        logger.info("\u001B[36;1m{}\u001B[0m", paddedMessage);
        System.out.println("\u001B[36;1m" + paddedMessage + "\u001B[0m");
    }
} 