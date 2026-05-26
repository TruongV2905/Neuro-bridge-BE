# Sử dụng JDK 21
FROM eclipse-temurin:21-jre-alpine

# Thư mục làm việc trong container
WORKDIR /app

# Copy file jar từ target (nhớ chạy mvn package trước khi build docker)
COPY target/*.jar app.jar

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]