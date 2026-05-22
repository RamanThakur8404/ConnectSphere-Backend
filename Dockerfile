FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ARG SERVICE_NAME

COPY ${SERVICE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
