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

## 技术栈

- Java 17+
- Maven
- OpenAI API
- Model Context Protocol (MCP)

## 依赖项

- com.theokanning.openai-gpt3-java:service - OpenAI API 客户端
- com.google.code.gson - JSON 处理
- io.github.cdimascio:dotenv-java - 环境变量处理
- okhttp3 - HTTP 客户端
- slf4j 和 logback - 日志记录

## 环境设置

1. 克隆仓库
2. 配置环境变量
   - 复制 `src/main/resources/.env.example` 到 `src/main/resources/.env`
   - 填写您的 API 密钥和其他配置

## 编译与运行

```bash
# 编译项目
mvn clean compile

# 打包
mvn package

# 运行
java -jar target/llm-mcp-rag-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## 与原 TypeScript 版本的区别

- 使用 Java 面向对象编程范式
- 使用 Maven 进行依赖管理（替代npm/yarn）
- 使用 com.theokanning.openai-gpt3-java 库代替原生JavaScript OpenAI SDK
- 使用 Java 自己的进程管理替代Node.js的进程管理

## 许可证

与原项目相同

## 贡献

欢迎提交问题和拉取请求。
