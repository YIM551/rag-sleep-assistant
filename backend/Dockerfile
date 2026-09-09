# Build stage
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy only necessary files for build
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./gradlew
RUN chmod +x ./gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build application (skip tests for faster build)
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM amazoncorretto:17-alpine
WORKDIR /app

# Install curl for healthcheck and timezone data (Alpine용)
RUN apk add --no-cache curl tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

# Copy jar file from build stage
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# Create logs and RAG Lucene index directories, and non-root user (Alpine용)
RUN mkdir -p /app/logs /app/build/rag-lucene && \
    addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=180s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run application with memory settings and timezone
# 명시적 heap 크기 설정 (t3.medium 3.7GB RAM 기준)
# - Xmx2800m: 최대 heap 2.8GB (RAM의 75%)
# - Xms1400m: 초기 heap 1.4GB (최대의 50%)
# - XX:+UseContainerSupport: 컨테이너 인식 (Java 10+)
ENTRYPOINT ["java", "-Xmx2800m", "-Xms1400m", "-XX:+UseContainerSupport", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]