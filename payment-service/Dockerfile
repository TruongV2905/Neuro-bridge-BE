FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 4564 9095
ENTRYPOINT ["java", "-jar", "app.jar"]
