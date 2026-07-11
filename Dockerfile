FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

COPY --from=build /workspace/target/paychecker-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
