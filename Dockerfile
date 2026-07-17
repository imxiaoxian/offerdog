FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy project files and build the Spring Boot jar
COPY pom.xml .
COPY .mvn .mvn
COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system spring && useradd --system --gid spring spring
WORKDIR /app

COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS=""
# 嵌入默认硅基流动（密钥勿写进镜像，由 compose / 运行时 -e 注入 SILICONFLOW_API_KEY 或 SPRING_AI_OPENAI_API_KEY）
ENV SILICONFLOW_BASE_URL=https://api.siliconflow.cn
ENV SILICONFLOW_EMBEDDING_MODEL=BAAI/bge-large-zh-v1.5

USER spring
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
