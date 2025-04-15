# LLM-MCP-RAG Java 版本

这是原 TypeScript LLM-MCP-RAG 项目的 Java 实现版本。该项目结合了大型语言模型（LLM）和检索增强生成（RAG）技术，通过 Model Context Protocol (MCP) 与外部工具进行交互。

## 项目结构

```
src/main/java/com/rag/
├── client/            # 客户端组件
│   ├── Agent.java     # 代理，协调LLM和工具
│   ├── ChatOpenAI.java # OpenAI接口
│   └── MCPClient.java  # MCP协议客户端
├── model/             # 模型组件
│   ├── EmbeddingRetriever.java # 嵌入检索
│   └── VectorStore.java  # 向量存储
├── utils/             # 工具类
│   └── LogUtils.java   # 日志工具
└── Main.java          # 程序入口
```

## 功能

1. **检索增强生成 (RAG)** - 使用向量嵌入从知识库中检索相关文档
2. **工具调用** - 通过MCP协议与外部工具交互
3. **LLM集成** - 集成OpenAI的大型语言模型

## 依赖项

- com.theokanning.openai-gpt3-java:service - OpenAI API 客户端
- com.google.code.gson - JSON 处理
- io.github.cdimascio:dotenv-java - 环境变量处理
- okhttp3 - HTTP 客户端
- slf4j 和 logback - 日志记录
  

## 编译与运行

```bash
# 编译项目
mvn clean compile

# 打包
mvn package

# 运行
java -jar target/llm-mcp-rag-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 进展
```
4月14日 在实现基本功能的基础上做了以下优化：1.上下文摘要 2.嵌入向量缓存 3.LLM响应缓存 4.上下文分块处理 5.另外实现了基于语义的较为智能的分块。还可以考虑引入NLP进行分词 6.增量式上下文更新
```
