FROM maven:3.9.11-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S mindcare \
    && adduser -S mindcare -G mindcare

WORKDIR /app

COPY --from=builder --chown=mindcare:mindcare /workspace/target/*.jar app.jar

USER mindcare

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
