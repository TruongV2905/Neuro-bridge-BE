FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy file jar đã build sẵn từ thư mục target ngoài máy thật vào
COPY target/*.jar app.jar
EXPOSE 4562
ENTRYPOINT ["java", "-jar", "app.jar"]